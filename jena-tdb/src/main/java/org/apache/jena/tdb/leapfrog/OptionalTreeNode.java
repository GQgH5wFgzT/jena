package org.apache.jena.tdb.leapfrog;

import java.util.Iterator;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpConditional;
import org.apache.jena.sparql.algebra.op.OpQuadPattern;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.optimizer.reorder.ReorderTransformation;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.tdb.solver.BindingNodeId;
import org.apache.jena.tdb.solver.BindingTDB;
import org.apache.jena.tdb.store.NodeId;
import org.apache.jena.tdb.store.nodetable.NodeTable;
import org.apache.jena.tdb.store.nodetupletable.NodeTupleTable;

import org.apache.jena.tdb.leapfrog.BGPIter;

public class OptionalTreeNode implements LFIterBindingNodeId {

    private OptionalTreeNode[] children;
    private BGPIter myPattern;
    private NodeTable nodeTable;

    private BindingNodeId myBinding;
    private ExprList preFilters;
    private ExprList postFilters;
    private BindingNodeId extendedBinding;

    private Var[] globalAttributeOrder;

    private boolean atEnd = false;
    private boolean atEndForever = false;
    private boolean nextFound = false;
    private int stage = STAGE_FIND;

    private final static int STAGE_FIND = 0;        // This stage tries to find a binding from myPattern
    private final static int STAGE_EXTEND = 1;      // This stage tries to extend myBinding with children bindings

    private final static int CHILDREN_CACHE_SIZE = 1000;

    private int[] cacheSize;
    private int[] cacheCurrentPos;
    private BindingNodeId[][] childrenCachedBindings;

    private OptionalTreeNode(NodeTupleTable ntt, BasicPattern pattern) {
        nodeTable = ntt.getNodeTable();
        myPattern = new BGPIter(pattern, ntt);
        extendedBinding = new BindingNodeId();
        preFilters = new ExprList();
        postFilters = new ExprList();
    }

    public static OptionalTreeNode getNode(NodeTupleTable ntt, Op op, ReorderTransformation reorderTransformation) {
        if (op instanceof OpBGP) {
            OpBGP opBGP = (OpBGP) op;
            return new OptionalTreeNode(ntt, reorderTransformation.reorder(opBGP.getPattern()));

        } else if (op instanceof OpQuadPattern) {
            OpQuadPattern opQuad = (OpQuadPattern) op;
            //if (!opQuad.isDefaultGraph()) {
            //    throw new IllegalStateException("No graph allowed for this leapfrog implementation.");
            //}
            return new OptionalTreeNode(ntt, reorderTransformation.reorder(opQuad.getBasicPattern()));

        } else if (op instanceof OpConditional) {
            OpConditional opConditional = (OpConditional) op;
            OptionalTreeNode leftNode = OptionalTreeNode.getNode(ntt, opConditional.getLeft(), reorderTransformation);
            OptionalTreeNode rightNode = OptionalTreeNode.getNode(ntt, opConditional.getRight(), reorderTransformation);
            leftNode.addChild(rightNode);
            return leftNode;

        } else if (op instanceof OpLeftJoin) {
            OpLeftJoin opLeftJoin = (OpLeftJoin) op;
            OptionalTreeNode leftNode = OptionalTreeNode.getNode(ntt, opLeftJoin.getLeft(), reorderTransformation);
            OptionalTreeNode rightNode = OptionalTreeNode.getNode(ntt, opLeftJoin.getRight(), reorderTransformation);
            leftNode.addChild(rightNode);
            return leftNode;

        } else if (op instanceof OpFilter) {
            OpFilter opFilter = (OpFilter) op;
            OptionalTreeNode res = OptionalTreeNode.getNode(ntt, opFilter.getSubOp(), reorderTransformation);
            res.addFilters(opFilter.getExprs());
            return res;

        } else {
            throw new IllegalStateException(op.getName() + " not supported");
        }

    }

    private void addFilters(ExprList filters) {
        for (Expr filter : filters) {
            boolean hasExternalVars = false;

            for (Var v : filter.getVarsMentioned()) {
                if (!myPattern.hasVariable(v)) {
                    hasExternalVars = true;
                    break;
                }
            }

            if (hasExternalVars) {
                postFilters.add(filter);
            } else {
                preFilters.add(filter);
            }
        }
    }

    public void addChild(OptionalTreeNode newChild) {
        if (children == null) {
            children = new OptionalTreeNode[1];
            children[0] = newChild;
        } else {
            OptionalTreeNode[] newChildren = new OptionalTreeNode[children.length + 1];
            for (int i = 0; i < children.length; i++) {
                newChildren[i] = children[i];
            }
            newChildren[children.length] = newChild;
            this.children = newChildren;
        }
    }

    /* This init call is only for the root */
    @Override
    public Var[] init(Var[] upperVars) {
        return init(upperVars, true);
    }

    private Var[] init(Var[] upperVars, boolean root) {
        if (children == null) {
            children = new OptionalTreeNode[0];
        }
        cacheSize = new int[children.length];
        cacheCurrentPos = new int[children.length];
        childrenCachedBindings = new BindingNodeId[children.length][];

        globalAttributeOrder = myPattern.init(upperVars);
        atEndForever = !myPattern.openTerms();
        atEnd = atEndForever;

        if (root) {
            myPattern.open();
        }

        for (int i = 0; i < children.length; i++) {
            childrenCachedBindings[i] = new BindingNodeId[CHILDREN_CACHE_SIZE];
            globalAttributeOrder = children[i].init(globalAttributeOrder, false);
        }

        return globalAttributeOrder;
    }

    private void reset() {
        stage = STAGE_FIND;
        myPattern.reset();
        atEnd = atEndForever;
    }

    @Override
    public boolean hasNext() {
        if (atEnd) {
            return false;
        } else if (nextFound) {
            return true;
        }
        mainLoop:
        while (true) {
            if (stage == STAGE_FIND) {
                if (myPattern.hasNext()) {
                    BindingNodeId myOldBinding = myBinding;
                    long[] myPatternIds = myPattern.next();
                    myBinding = new BindingNodeId();
                    for (int i = 0; i < myPatternIds.length; i++) {
                        myBinding.put(myPattern.getLocalAttribute(i), NodeId.create(myPatternIds[i]));
                    }
                    extendedBinding = new BindingNodeId(myBinding);
                    if (!checkFilters(preFilters)) {
                        continue mainLoop;
                    }
                    if (children.length > 0) {
                        int firstChangedVarPos = myPattern.getFirstChangedVar(myOldBinding);
                        for (int i = 0; i < children.length; i++) {
                            if (children[i].seekBinding(myBinding, firstChangedVarPos) && children[i].hasNext()) {
                                stage = STAGE_EXTEND;
                                fillChildCache(i);
                                extendBinding(i);
                            } else {
                                cacheSize[i] = 0;
                            }
                        }
                    }
                    if (!checkFilters(postFilters)) {
                        continue mainLoop;
                    }
                    nextFound = true;
                    return true;
                } else {
                    atEnd = true;
                    return false;
                }
            } else {
                boolean extendStageResult = false;
                extendStageLoop:
                while (true) {
                    for (int i = 0; i < children.length; i++) {
                        if (cacheCurrentPos[i] + 1 < cacheSize[i]) {
                            cacheCurrentPos[i]++;
                            extendBinding(i);
                            while (--i >= 0) {
                                resetChildCache(i);
                            }
                            nextFound = true;
                            extendStageResult = true;
                            break extendStageLoop;
                        }
                        if (children[i].hasNext()) {
                            fillChildCache(i);
                            extendBinding(i);
                            while (--i >= 0) {
                                resetChildCache(i);
                            }
                            nextFound = true;
                            extendStageResult = true;
                            break extendStageLoop;
                        }
                    }
                    stage = STAGE_FIND;
                    for (int i = 0; i < children.length; i++) {
                        children[i].atEnd = children[i].atEndForever; //BUG
                    }
                    continue mainLoop;
                }
                if (extendStageResult) {
                    if (!checkFilters(postFilters)) {
                        continue mainLoop;
                    }
                    nextFound = true;
                    return true;
                } else {
                    atEnd = true;
                    return false;
                }
            }
        }
    }

    private void extendBinding(int childPos) {
        if (this.cacheSize[childPos] > 0) {
            BindingNodeId binding = childrenCachedBindings[childPos][cacheCurrentPos[childPos]];
            Iterator<Var> bindingVars = binding.iterator();
            while (bindingVars.hasNext()) {
                Var v = bindingVars.next();
                if (!myBinding.containsKey(v)) {
                    extendedBinding.put(v, binding.get(v));
                }
            }
        }
    }

    private void fillChildCache(int childPos) {
        cacheCurrentPos[childPos] = 0;
        int size = 0;
        while (children[childPos].hasNext() && size < CHILDREN_CACHE_SIZE) {
            childrenCachedBindings[childPos][size] = children[childPos].next();
            size++;
        }
        cacheSize[childPos] = size;
    }

    private void resetChildCache(int childPos) {
        children[childPos].reset();
        fillChildCache(childPos);
        extendBinding(childPos);
    }

    @Override
    public BindingNodeId next() {
        nextFound = false;
        return extendedBinding;
    }

    @Override
    public boolean seekBinding(BindingNodeId previousBinding, int firstChangedVarPos) {
        if(atEndForever) return false;
        atEnd = !myPattern.seekBinding(previousBinding, firstChangedVarPos);
        return !atEnd;
    }
    
    private boolean checkFilters(ExprList filters) {
        if (filters.size() > 0) {
            Binding binding = new BindingTDB(extendedBinding, nodeTable);

            for (Expr filter : filters) {
                if (!filter.isSatisfied(binding, null)) {
                    return false;
                }
            }
        }
        return true;
    }

}

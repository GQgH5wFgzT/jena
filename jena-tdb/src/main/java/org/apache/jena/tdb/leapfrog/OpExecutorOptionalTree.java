package org.apache.jena.tdb.leapfrog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpConditional;
import org.apache.jena.sparql.algebra.op.OpQuadPattern;
import org.apache.jena.sparql.algebra.op.OpSequence;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.main.OpExecutor;
import org.apache.jena.sparql.engine.main.OpExecutorFactory;
import org.apache.jena.sparql.engine.optimizer.reorder.ReorderTransformation;
import org.apache.jena.tdb.leapfrog.LFIterBindingNodeId;
import org.apache.jena.tdb.leapfrog.OptionalTreeNode;
import org.apache.jena.tdb.solver.Abortable;
import org.apache.jena.tdb.solver.BindingNodeId;
import org.apache.jena.tdb.solver.QueryIterTDB;
import org.apache.jena.tdb.solver.SolverLib;
import org.apache.jena.tdb.store.GraphTDB;
import org.apache.jena.tdb.store.nodetable.NodeTable;
import org.apache.jena.tdb.store.nodetupletable.NodeTupleTable;

public class OpExecutorOptionalTree extends OpExecutor { // TODO ver que todos los operadores funcionen

    private ReorderTransformation reorderTransformation;

    public final static OpExecutorFactory OpExecFactoryOptionalTree = new OpExecutorFactory() {
        @Override
        public OpExecutor create(ExecutionContext execCxt) {
            return new OpExecutorOptionalTree(execCxt);
        }
    };

    protected OpExecutorOptionalTree(ExecutionContext execCxt) {
        super(execCxt);
        GraphTDB graphTDB = (GraphTDB) execCxt.getActiveGraph();
        reorderTransformation = graphTDB.getDatasetGraphTDB().getReorderTransform();
    }

    @Override
    protected QueryIterator execute(OpSequence opSequence, QueryIterator input) {
        GraphTDB graph = (GraphTDB) execCxt.getActiveGraph();
        List<Abortable> killList = new ArrayList<>();

        Node graphNode = null;
        NodeTupleTable ntt = graph.getDatasetGraphTDB().chooseNodeTupleTable(graphNode);
        NodeTable nodeTable = ntt.getNodeTable();

        LFIterBindingNodeId iter = OptionalTreeNode.getNode(ntt, opSequence, reorderTransformation);
        iter.init(new Var[0]);

        Iterator<BindingNodeId> abortableIter = SolverLib.makeAbortable(iter, killList);
        Iterator<Binding> iterBinding = SolverLib.convertToNodes(abortableIter, nodeTable);

        return new QueryIterTDB(iterBinding, killList, input, execCxt);
    }

    @Override
    protected QueryIterator execute(OpFilter opFilter, QueryIterator input) {
        GraphTDB graph = (GraphTDB) execCxt.getActiveGraph();
        List<Abortable> killList = new ArrayList<>();

        Node graphNode = null;
        NodeTupleTable ntt = graph.getDatasetGraphTDB().chooseNodeTupleTable(graphNode);
        NodeTable nodeTable = ntt.getNodeTable();

        LFIterBindingNodeId iter = OptionalTreeNode.getNode(ntt, opFilter, reorderTransformation);
        iter.init(new Var[0]);

        Iterator<BindingNodeId> abortableIter = SolverLib.makeAbortable(iter, killList);
        Iterator<Binding> iterBinding = SolverLib.convertToNodes(abortableIter, nodeTable);

        return new QueryIterTDB(iterBinding, killList, input, execCxt);
    }

    @Override
    protected QueryIterator execute(OpBGP opBGP, QueryIterator input) {
        GraphTDB graph = (GraphTDB) execCxt.getActiveGraph();
        List<Abortable> killList = new ArrayList<>();

        Node graphNode = null;
        NodeTupleTable ntt = graph.getDatasetGraphTDB().chooseNodeTupleTable(graphNode);
        NodeTable nodeTable = ntt.getNodeTable();

        LFIterBindingNodeId iter = OptionalTreeNode.getNode(ntt, opBGP, reorderTransformation);
        iter.init(new Var[0]);

        Iterator<BindingNodeId> abortableIter = SolverLib.makeAbortable(iter, killList);
        Iterator<Binding> iterBinding = SolverLib.convertToNodes(abortableIter, nodeTable);
        return new QueryIterTDB(iterBinding, killList, input, execCxt);
    }

    @Override
    protected QueryIterator execute(OpQuadPattern opQuadPattern, QueryIterator input) {
        GraphTDB graph = (GraphTDB) execCxt.getActiveGraph();
        List<Abortable> killList = new ArrayList<>();

        Node graphNode = null;
        NodeTupleTable ntt = graph.getDatasetGraphTDB().chooseNodeTupleTable(graphNode);
        NodeTable nodeTable = ntt.getNodeTable();

        LFIterBindingNodeId iter = OptionalTreeNode.getNode(ntt, opQuadPattern, reorderTransformation);
        iter.init(new Var[0]);

        Iterator<BindingNodeId> abortableIter = SolverLib.makeAbortable(iter, killList);
        Iterator<Binding> iterBinding = SolverLib.convertToNodes(abortableIter, nodeTable);
        return new QueryIterTDB(iterBinding, killList, input, execCxt);
    }

    @Override
    protected QueryIterator execute(OpConditional opConditional, QueryIterator input) {
        GraphTDB graph = (GraphTDB) execCxt.getActiveGraph();
        List<Abortable> killList = new ArrayList<>();

        Node graphNode = null;
        NodeTupleTable ntt = graph.getDatasetGraphTDB().chooseNodeTupleTable(graphNode);
        NodeTable nodeTable = ntt.getNodeTable();

        LFIterBindingNodeId iter = OptionalTreeNode.getNode(ntt, opConditional, reorderTransformation);
        iter.init(new Var[0]);

        Iterator<BindingNodeId> abortableIter = SolverLib.makeAbortable(iter, killList);
        Iterator<Binding> iterBinding = SolverLib.convertToNodes(abortableIter, nodeTable);
        return new QueryIterTDB(iterBinding, killList, input, execCxt);
    }
    
    @Override
    protected QueryIterator execute(OpLeftJoin opLeftJoin, QueryIterator input) {
        GraphTDB graph = (GraphTDB) execCxt.getActiveGraph();
        List<Abortable> killList = new ArrayList<>();

        Node graphNode = null;
        NodeTupleTable ntt = graph.getDatasetGraphTDB().chooseNodeTupleTable(graphNode);
        NodeTable nodeTable = ntt.getNodeTable();

        LFIterBindingNodeId iter = OptionalTreeNode.getNode(ntt, opLeftJoin, reorderTransformation);
        iter.init(new Var[0]);

        Iterator<BindingNodeId> abortableIter = SolverLib.makeAbortable(iter, killList);
        Iterator<Binding> iterBinding = SolverLib.convertToNodes(abortableIter, nodeTable);
        return new QueryIterTDB(iterBinding, killList, input, execCxt);
    }

}

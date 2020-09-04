package io.rheem.iejoin.operators;

import io.rheem.core.function.TransformationDescriptor;
import io.rheem.core.optimizer.OptimizationContext;
import io.rheem.core.plan.rheemplan.ExecutionOperator;
import io.rheem.core.platform.ChannelDescriptor;
import io.rheem.core.platform.ChannelInstance;
import io.rheem.core.platform.lineage.ExecutionLineageNode;
import io.rheem.core.types.DataSetType;
import io.rheem.core.util.Tuple;
import io.rheem.iejoin.data.Data;
import io.rheem.iejoin.operators.java_helpers.BitSetJoin;
import io.rheem.iejoin.operators.java_helpers.DataComparator;
import io.rheem.iejoin.operators.java_helpers.extractData;
import io.rheem.java.channels.CollectionChannel;
import io.rheem.java.channels.JavaChannelInstance;
import io.rheem.java.channels.StreamChannel;
import io.rheem.java.execution.JavaExecutor;
import io.rheem.java.operators.JavaExecutionOperator;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Java implementation of the {@link IESelfJoinOperator}.
 */
public class JavaIESelfJoinOperator<Type0 extends Comparable<Type0>, Type1 extends Comparable<Type1>, Input>
        extends IESelfJoinOperator<Type0, Type1, Input>
        implements JavaExecutionOperator {

    /**
     * Creates a new instance.
     */
    public JavaIESelfJoinOperator(DataSetType<Input> inputType0,
                                  TransformationDescriptor<Input, Type0> get0Pivot, IEJoinMasterOperator.JoinCondition cond0,
                                  TransformationDescriptor<Input, Type1> get0Ref, IEJoinMasterOperator.JoinCondition cond1) {
        super(inputType0, get0Pivot, cond0, get0Ref, cond1);
    }

    @Override
    public Tuple<Collection<ExecutionLineageNode>, Collection<ChannelInstance>> evaluate(
            ChannelInstance[] inputs,
            ChannelInstance[] outputs,
            JavaExecutor javaExecutor,
            OptimizationContext.OperatorContext operatorContext) {
        StreamChannel.Instance outputChannel = (StreamChannel.Instance) outputs[0];

        Stream<Input> stream0;
        if (inputs[0] instanceof CollectionChannel.Instance) {
            final Collection<Input> collection = ((CollectionChannel.Instance) inputs[0]).provideCollection();
            stream0 = collection.stream();
        } else {
            // Fallback: Materialize one side.
            final Collection<Input> collection = ((JavaChannelInstance) inputs[0]).<Input>provideStream().collect(Collectors.toList());
            stream0 = collection.stream();
        }

        Object[] stream0R = stream0.toArray();

        ArrayList<Tuple2<Data<Type0, Type1>, Input>> list0 = new ArrayList<>();

        final Function<Input, Type0> get0Pivot_ = javaExecutor.getCompiler().compile(this.get0Pivot);
        final Function<Input, Type1> get0Ref_ = javaExecutor.getCompiler().compile(this.get0Ref);

        for (int i = 0; i < stream0R.length; i++) {
            list0.add(new Tuple2<>(new extractData<>(get0Pivot_, get0Ref_).call((Input) stream0R[i]), (Input) stream0R[i]));
        }

        Collections.sort(list0, new DataComparator<>(list1ASC, list1ASCSec));

        long partCount = list0.size();

        // Give unique ID for rdd1
        for (int i = 0; i < partCount; i++) {
            list0.get(i)._1().setRowID(i);
        }

        ArrayList<Tuple2<Input, Input>> result = new BitSetJoin<Type0, Type1, Input>(list1ASC, list2ASC,
                list1ASCSec, list2ASCSec, equalReverse, true, cond0).call(list0, list0);

        ArrayList<io.rheem.basic.data.Tuple2<Input, Input>> result2 = new ArrayList<>();
        for (Tuple2<Input, Input> t : result) {
            result2.add(new io.rheem.basic.data.Tuple2<Input, Input>(t._1(), t._2()));
        }

        outputChannel.<io.rheem.basic.data.Tuple2<Input, Input>>accept(result2.stream());

        return ExecutionOperator.modelEagerExecution(inputs, outputs, operatorContext);
    }

    @Override
    protected ExecutionOperator createCopy() {
        return new JavaIESelfJoinOperator<Type0, Type1, Input>(this.getInputType(),
                get0Pivot, cond0, get0Ref, cond1);
    }

    @Override
    public List<ChannelDescriptor> getSupportedInputChannels(int index) {
        assert index <= this.getNumInputs() || (index == 0 && this.getNumInputs() == 0);
        return Arrays.asList(CollectionChannel.DESCRIPTOR, StreamChannel.DESCRIPTOR);
    }

    @Override
    public List<ChannelDescriptor> getSupportedOutputChannels(int index) {
        assert index <= this.getNumOutputs() || (index == 0 && this.getNumOutputs() == 0);
        return Collections.singletonList(StreamChannel.DESCRIPTOR);
    }
}

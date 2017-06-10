package org.jenkinsci.plugins.mqpipelinenotifier;


import com.google.common.util.concurrent.ListenableFuture;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import io.jenkins.blueocean.events.PipelineEventChannel;
import io.jenkins.blueocean.rest.impl.pipeline.PipelineInputStepListener;
import io.jenkins.blueocean.rest.impl.pipeline.PipelineNodeUtil;
import org.jenkinsci.plugins.pubsub.*;
import org.jenkinsci.plugins.workflow.actions.BodyInvocationAction;
import org.jenkinsci.plugins.workflow.actions.StageAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowEndNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.StepNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.input.InputAction;
import org.jenkinsci.plugins.workflow.support.steps.input.InputStep;
import io.jenkins.blueocean.events.PipelineEventChannel.Event;
import io.jenkins.blueocean.events.PipelineEventChannel.EventProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Created by qeesung on 2017/6/10.
 */
@Extension
public class StageEventRunListenerImpl extends RunListener<Run<?, ?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StageEventRunListenerImpl.class);
    private ExecutorService executor;

    public StageEventRunListenerImpl() {
        this.executor = new ThreadPoolExecutor(0, 5, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue());
    }

    public void onStarted(final Run<?, ?> run, TaskListener listener) {
        super.onStarted(run, listener);
        if (run instanceof WorkflowRun) {
            ListenableFuture promise = ((WorkflowRun) run).getExecutionPromise();
            promise.addListener(new Runnable() {
                public void run() {
                    try {
                        FlowExecution e = (FlowExecution) ((WorkflowRun) run).getExecutionPromise().get();
                        e.addListener(StageEventRunListenerImpl.this.new StageEventPublisher(run));
                    } catch (Exception var2) {
                        StageEventRunListenerImpl.LOGGER.error("Unexpected error publishing pipeline FlowNode event.", var2);
                    }

                }
            }, this.executor);
        }

    }

    @Extension
    public static class InputStepPublisher implements PipelineInputStepListener {
        public InputStepPublisher() {
        }

        public void onStepContinue(InputStep inputStep, WorkflowRun run) {
            try {
                PubsubBus.getBus().publish((new RunMessage(run)).setEventName(Events.JobChannel.job_run_unpaused));
            } catch (MessageException var4) {
                StageEventRunListenerImpl.LOGGER.warn("Error publishing Run un-pause event.", var4);
            }

        }
    }

    private class StageEventPublisher implements GraphListener {
        private final Run run;
        private final PubsubBus pubSubBus;
        private String currentStageName;
        private String currentStageId;

        public StageEventPublisher(Run r) {
            this.run = r;
            this.pubSubBus = PubsubBus.getBus();
            this.publishEvent(this.newMessage(Event.pipeline_start));
        }

        public void onNewHead(FlowNode flowNode) {
            List e;
            if (PipelineNodeUtil.isStage(flowNode)) {
                e = this.getBranch(flowNode);
                this.currentStageName = flowNode.getDisplayName();
                this.currentStageId = flowNode.getId();
                this.publishEvent(this.newMessage(PipelineEventChannel.Event.pipeline_stage, flowNode, e));
            } else if (flowNode instanceof StepStartNode) {
                if (flowNode.getAction(BodyInvocationAction.class) != null) {
                    e = this.getBranch(flowNode);
                    e.add(flowNode.getId());
                    this.publishEvent(this.newMessage(Event.pipeline_block_start, flowNode, e));
                }
            } else if (flowNode instanceof StepAtomNode) {
                e = this.getBranch(flowNode);
                StageAction startNode = (StageAction) flowNode.getAction(StageAction.class);
                this.publishEvent(this.newMessage(Event.pipeline_step, flowNode, e));
            } else if (flowNode instanceof StepEndNode) {
                if (flowNode.getAction(BodyInvocationAction.class) != null) {
                    try {
                        String e1 = ((StepStartNode) ((StepEndNode) flowNode).getStartNode()).getId();
                        FlowNode startNode1 = flowNode.getExecution().getNode(e1);
                        List branch = this.getBranch(startNode1);
                        branch.add(e1);
                        this.publishEvent(this.newMessage(Event.pipeline_block_end, flowNode, branch));
                    } catch (IOException var5) {
                        StageEventRunListenerImpl.LOGGER.error("Unexpected error publishing pipeline FlowNode event.", var5);
                    }
                }
            } else if (flowNode instanceof FlowEndNode) {
                this.publishEvent(this.newMessage(Event.pipeline_end));
            }

        }

        private List<String> getBranch(FlowNode flowNode) {
            ArrayList branch = new ArrayList();

            for (FlowNode parentBlock = this.getParentBlock(flowNode); parentBlock != null; parentBlock = this.getParentBlock(parentBlock)) {
                branch.add(0, parentBlock.getId());
            }

            return branch;
        }

        private FlowNode getParentBlock(FlowNode flowNode) {
            List parents = flowNode.getParents();
            Iterator var3 = parents.iterator();

            FlowNode parent;
            do {
                if (!var3.hasNext()) {
                    var3 = parents.iterator();

                    while (var3.hasNext()) {
                        parent = (FlowNode) var3.next();
                        if (!(parent instanceof StepEndNode)) {
                            FlowNode grandparent = this.getParentBlock(parent);
                            if (grandparent != null) {
                                return grandparent;
                            }
                        }
                    }

                    return null;
                }

                parent = (FlowNode) var3.next();
            } while (!(parent instanceof StepStartNode) || parent.getAction(BodyInvocationAction.class) == null);

            return parent;
        }

        private String toPath(List<String> branch) {
            StringBuilder builder = new StringBuilder();

            String leaf;
            for (Iterator var3 = branch.iterator(); var3.hasNext(); builder.append(leaf)) {
                leaf = (String) var3.next();
                if (builder.length() > 0) {
                    builder.append("/");
                }
            }

            return builder.toString();
        }

        private Message newMessage(Event event) {
            return ((SimpleMessage) ((SimpleMessage) ((SimpleMessage) (new SimpleMessage()).setChannelName("pipeline")).setEventName(event)).set(EventProps.pipeline_job_name, this.run.getParent().getFullName())).set(EventProps.pipeline_run_id, this.run.getId());
        }

        private Message newMessage(Event event, FlowNode flowNode, List<String> branch) {
            Message message = this.newMessage(event);
            message.set(EventProps.pipeline_step_flownode_id, flowNode.getId());
            message.set(EventProps.pipeline_context, this.toPath(branch));
            if (this.currentStageName != null) {
                message.set(EventProps.pipeline_step_stage_name, this.currentStageName);
                message.set(EventProps.pipeline_step_stage_id, this.currentStageId);
            }

            if (flowNode instanceof StepNode) {
                StepNode pausedForInputStep = (StepNode) flowNode;
                message.set(EventProps.pipeline_step_name, pausedForInputStep.getDescriptor().getFunctionName());
            }

            if (flowNode instanceof StepAtomNode) {
                boolean pausedForInputStep1 = PipelineNodeUtil.isPausedForInputStep((StepAtomNode) flowNode, (InputAction) this.run.getAction(InputAction.class));
                if (pausedForInputStep1) {
                    try {
                        PubsubBus.getBus().publish((new RunMessage(this.run)).setEventName(Events.JobChannel.job_run_paused));
                    } catch (MessageException var7) {
                        StageEventRunListenerImpl.LOGGER.warn("Error publishing Run pause event.", var7);
                    }
                }

                message.set(EventProps.pipeline_step_is_paused, String.valueOf(pausedForInputStep1));
            }

            return message;
        }

        private void publishEvent(Message message) {
            System.out.println("================");
            System.out.println(message);
            System.out.println("++++++++++++++++");
        }
    }
}

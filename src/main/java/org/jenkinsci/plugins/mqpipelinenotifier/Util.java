package org.jenkinsci.plugins.mqpipelinenotifier;

/**
 * Created by qeesung on 2017/6/10.
 */
import hudson.Functions;
import hudson.model.AbstractItem;
import hudson.model.Queue;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Constants and helper functions.
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
public final class Util {
    private static String hostName = null;

    /**Url Key. */
    public static final String KEY_URL = "url";
    /**Name Key. */
    public static final String KEY_PROJECT_NAME = "build_job_name";
    /**BuildNr Key. */
    public static final String KEY_BUILD_NR = "build_number";
    /**Master FQDN Key. */
    public static final String KEY_MASTER_FQDN = "jenkins_master_fqdn";
    /**State Key. */
    public static final String KEY_STATE = "state";
    /**Dequeue Reason Key. */
    public static final String KEY_DEQUEUE_REASON = "dequeue_reason";
    /**Dequeue Time Spent in Queue in ms. */
    public static final String KEY_DEQUEUE_TIME_SPENT = "time_spent_in_queue";
    /**Dequeue Allocated Label. */
    public static final String KEY_DEQUEUE_ALLOCATED_LABEL = "allocated_label";
    /**Status Key. */
    public static final String KEY_STATUS = "status";
    /**Dequeue No Label. */
    public static final String VALUE_DEQUEUE_NO_LABEL = "NO_LABEL";
    /**Unknown host Value. */
    public static final String VALUE_UNRESOLVED_HOST = "unknown_host";
    /**Queued Value. */
    public static final String VALUE_ADDED_TO_QUEUE = "QUEUED";
    /**Dequeued Value. */
    public static final String VALUE_REMOVED_FROM_QUEUE = "DEQUEUED";
    /**Cancelled Value. */
    public static final String VALUE_CANCELLED = "CANCELLED";
    /**Building Value. */
    public static final String VALUE_BUILDING = "BUILDING";
    /**Started Value. */
    public static final String VALUE_STARTED = "STARTED";
    /**Completed Value. */
    public static final String VALUE_COMPLETED = "COMPLETED";
    /**Deleted Value. */
    public static final String VALUE_DELETED = "DELETED";
    /**Content Type. */
    public static final String CONTENT_TYPE = "application/json";

    /**
     * Utility classes should not have a public or default constructor.
     */
    private Util() {
    }

    /**
     * Get the job url for use with the REST api.
     *
     * @param item The queue item.
     * @return The url.
     *
     */
    public static String getJobUrl(Queue.Item item) {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null && jenkins.getRootUrl() != null) {
            return Functions.joinPath(jenkins.getRootUrl(), item.task.getUrl());
        } else {
            return item.task.getUrl();
        }
    }

    /**
     * Get the job url for use with the REST api.
     *
     * @param r The started build.
     * @return The url.
     *
     */
    public static String getJobUrl(Run r) {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null && jenkins.getRootUrl() != null) {
            return Functions.joinPath(jenkins.getRootUrl(), r.getUrl());
        } else {
            return r.getUrl();
        }
    }

    /**
     * Fetches and caches the jenkins master FQDN.
     *
     * @return hostname
     */
    public static String getHostName() {
        if (hostName == null) {
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                return VALUE_UNRESOLVED_HOST;
            }
        }
        return hostName;
    }

    /**
     * Fetches the full name task name if available.
     *
     * @param t task
     * @return full name if available, else the short name
     */
    public static String getFullName(Queue.Task t) {
        if (t instanceof AbstractItem) {
            return ((AbstractItem)t).getFullName();
        } else {
            return t.getName();
        }
    }
}

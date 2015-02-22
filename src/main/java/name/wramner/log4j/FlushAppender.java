package name.wramner.log4j;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.helpers.AppenderAttachableImpl;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This appender watches for a log message that contains a specific text OR for log messages that simply exceeds the
 * configured threshold and flushes all attached appenders when found.
 * <p>
 * Options:
 * <ul>
 * <li>OneShot - when enabled the attached appenders are left unbuffered when they have been flush. This is good if the
 * appender is used to switch buffered appenders to unbuffered at system shutdown, so that no messages are lost. This is
 * the default. Set to false in order to restore the buffers after flush.</li>
 * <li>TriggerMessage - the message text to look for (log message contains text) in order to flush. This is null by
 * default, in which case all messages that exceed the configured threshold for the appender cause a flush.</li>
 * <li>FlushMessage - the message text to send to attached loggers in order to flush them.</li>
 * </ul>
 * 
 * @author Erik Wramner
 */
public class FlushAppender extends AppenderSkeleton implements AppenderAttachable {

    private static final String DEFAULT_FLUSH_MESSAGE = "*** FLUSH ***";
    private String flushMessage = DEFAULT_FLUSH_MESSAGE;
    private String triggerMessage;
    private boolean oneShot = true;
    private final AppenderAttachableImpl appenders = new AppenderAttachableImpl();

    /**
     * Check if event contains flush message or if no message has been set - if so flush all attached appenders.
     * 
     * @param event The event.
     */
    @Override
    protected void append(LoggingEvent event) {
        String triggerMessage = getTriggerMessage();
        if (triggerMessage == null
                || (event.getMessage() != null && event.getMessage().toString().contains(triggerMessage))) {
            flushAppenders();
        }
    }

    /**
     * Close appender and all attached appenders.
     */
    @Override
    public void close() {
        synchronized (appenders) {
            for (Enumeration<?> e = appenders.getAllAppenders(); e.hasMoreElements();) {
                ((Appender) e.nextElement()).close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requiresLayout() {
        return false;
    }

    /**
     * Get the trigger message property.
     * 
     * @return log message text (partial) that activates a flush.
     */
    public synchronized String getTriggerMessage() {
        return triggerMessage;
    }

    /**
     * Set the trigger message property. Note that null means always flush!
     * 
     * @param triggerMessage The log message that activates a flush (contains).
     */
    public synchronized void setTriggerMessage(String message) {
        this.triggerMessage = message;
    }

    /**
     * Get the message to write to attached appenders.
     * 
     * @return message.
     */
    public synchronized String getFlushMessage() {
        return flushMessage;
    }

    /**
     * Set the message to write to attached appenders. If the message is null the default flush message will be used.
     * 
     * @param message The new message.
     */
    public synchronized void setFlushMessage(String message) {
        this.flushMessage = message != null ? message : DEFAULT_FLUSH_MESSAGE;
    }

    /**
     * Check if buffering should remain disabled after flush.
     * 
     * @return true if buffering remains off after first flush.
     */
    public synchronized boolean isOneShot() {
        return oneShot;
    }

    /**
     * Configure if buffering should remain off after flush.
     * 
     * @param oneShot The flag to keep buffering off or not.
     */
    public synchronized void setOneShot(boolean oneShot) {
        this.oneShot = oneShot;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAppender(Appender appender) {
        synchronized (appenders) {
            appenders.addAppender(appender);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getAllAppenders() {
        synchronized (appenders) {
            return appenders.getAllAppenders();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Appender getAppender(String name) {
        synchronized (appenders) {
            return appenders.getAppender(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAttached(Appender appender) {
        synchronized (appenders) {
            return appenders.isAttached(appender);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllAppenders() {
        synchronized (appenders) {
            appenders.removeAllAppenders();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAppender(Appender appender) {
        synchronized (appenders) {
            appenders.removeAppender(appender);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAppender(String name) {
        synchronized (appenders) {
            appenders.removeAppender(name);
        }
    }

    private void flushAppenders() {
        flushAppenders(getAllAppenders(), new LoggingEvent(getClass().getName(), LogManager.getRootLogger(), Level.ALL,
                getFlushMessage(), null), !isOneShot(), new HashSet<Appender>());
    }

    private void flushAppenders(Enumeration<?> appenderEnumeration, LoggingEvent event, boolean restoreBuffering,
            Set<Appender> alreadyFlushed) {
        while (appenderEnumeration.hasMoreElements()) {
            Object appender = appenderEnumeration.nextElement();
            if (!alreadyFlushed.contains(appender)) {
                alreadyFlushed.add((Appender) appender);

                if (appender instanceof AppenderAttachable) {
                    AppenderAttachable aa = (AppenderAttachable) appender;
                    flushAppenders(aa.getAllAppenders(), event, restoreBuffering, alreadyFlushed);
                }
                if (appender instanceof FileAppender) {
                    flushFileAppender((FileAppender) appender, event, restoreBuffering);
                }
            }
        }
    }

    private void flushFileAppender(FileAppender fp, LoggingEvent event, boolean restoreBuffering) {
        if (fp.getBufferedIO()) {
            fp.setBufferedIO(false);
            fp.setImmediateFlush(true);
            fp.append(event);
            if (restoreBuffering) {
                fp.setBufferedIO(true);
                fp.setImmediateFlush(false);
            }
        }
    }
}

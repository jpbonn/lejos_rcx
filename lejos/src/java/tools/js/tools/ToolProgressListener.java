package js.tools;

/**
 * Listener for progress of long running operations.
 */
public interface ToolProgressListener
{
  /**
   * Notification change of operation.
   * 
   * @param message human readable description of current operation
   */
  public void operation (String message);

  /**
   * Notification about some progress information.
   * 
   * @param message human readable message about progress
   */
  public void log (String message);

  /**
   * Notification about progress.
   * 
   * @param int progress progress (0-100)
   */
  public void progress (int progress);
}

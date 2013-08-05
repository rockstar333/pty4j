package com.pty4j.windows;

import com.pty4j.WinSize;
import com.sun.jna.*;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import jtermios.windows.WinAPI;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.Buffer;
import java.security.CodeSource;

/**
 * @author traff
 */
public class WinPty {
  private final winpty_t my_winpty;

  public WinPty(String cmdline, String cwd, String env) {
    my_winpty = INSTANCE.winpty_open(80, 25);

    if (my_winpty == null) {
      throw new IllegalStateException("winpty is null");
    }

    int c;

    char[] cmdlineArray = cmdline != null? toCharArray(cmdline): null;
    char[] cwdArray = cwd != null? toCharArray(cwd): null;
    char[] envArray = env != null? toCharArray(env): null;

    if ((c = INSTANCE.winpty_start_process(my_winpty, null, cmdlineArray, cwdArray, envArray)) != 0) {
      throw new IllegalStateException("Error running process:" + c);
    }
  }

  private char[] toCharArray(String cmdline) {
    char[] array = new char[cmdline.length() + 1];
    System.arraycopy(cmdline.toCharArray(), 0, array, 0, cmdline.length());
    array[cmdline.length()] = 0;
    return array;
  }

  public void setWinSize(WinSize winSize) {
    INSTANCE.winpty_set_size(my_winpty, winSize.ws_col, winSize.ws_row);
  }

  public void close() {
    INSTANCE.winpty_close(my_winpty);
  }

  public int exitValue() {
    return INSTANCE.winpty_get_exit_code(my_winpty);
  }

  public static class winpty_t extends Structure {
    public WinNT.HANDLE controlPipe;
    public WinNT.HANDLE dataPipe;
  }

  public WinNT.HANDLE getDataHandle() {
    return my_winpty.dataPipe;
  }

  public static final Kern32 KERNEL32 = (Kern32) Native.loadLibrary("kernel32", Kern32.class);

  static interface Kern32 extends Library {
    boolean PeekNamedPipe(WinNT.HANDLE hFile,
                          Buffer lpBuffer, 
                          int nBufferSize,
                          IntByReference lpBytesRead,
                          IntByReference lpTotalBytesAvail, 
                          IntByReference lpBytesLeftThisMessage);
  }
  
  static {
    System.setProperty("jna.library.path", getJarFolder());
  }

  private static String getJarFolder() {
    CodeSource codeSource = WinPty.class.getProtectionDomain().getCodeSource();
    File jarFile = null;
    try {
      jarFile = new File(codeSource.getLocation().toURI());
    } catch (URISyntaxException e) {
      return null;
    }
    return jarFile.getParentFile().getPath();
  }

  public static final WinPtyLib INSTANCE = (WinPtyLib) Native.loadLibrary("libwinpty", WinPtyLib.class);

  static interface WinPtyLib extends Library {
    /*
 * winpty API.
 */

    /*
     * Starts a new winpty instance with the given size.
     *
     * This function creates a new agent process and connects to it.
     */
    winpty_t winpty_open(int cols, int rows);

    /*
     * Start a child process.  Either (but not both) of appname and cmdline may
     * be NULL.  cwd and env may be NULL.  env is a pointer to an environment
     * block like that passed to CreateProcess.
     *
     * This function never modifies the cmdline, unlike CreateProcess.
     *
     * Only one child process may be started.  After the child process exits, the
     * agent will scrape the console output one last time, then close the data pipe
     * once all remaining data has been sent.
     *
     * Returns 0 on success or a Win32 error code on failure.
     */
    int winpty_start_process(winpty_t pc,
                             char[] appname,
                             char[] cmdline,
                             char[] cwd,
                             char[] env);

    /*
     * Returns the exit code of the process started with winpty_start_process,
     * or -1 none is available.
     */
    int winpty_get_exit_code(winpty_t pc);

    /*
     * Returns an overlapped-mode pipe handle that can be read and written
     * like a Unix terminal.
     */
    WinAPI.HANDLE winpty_get_data_pipe(winpty_t pc);

    /*
     * Change the size of the Windows console.
     */
    int winpty_set_size(winpty_t pc, int cols, int rows);

    /*
     * Closes the winpty.
     */
    void winpty_close(winpty_t pc);
  }


}
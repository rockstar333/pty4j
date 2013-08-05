package com.pty4j.windows;

import com.pty4j.PtyProcess;
import com.pty4j.util.Util;
import com.pty4j.WinSize;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author traff
 */
public class WinPtyProcess extends PtyProcess {
  private final WinPty myWinPty;
  private final WinPTYInputStream myInputStream;
  private final WinPTYOutputStream myOutputStream;


  public WinPtyProcess(String[] command, String[] environment, File workingDirectory) {
    myWinPty = new WinPty(Util.join(command, " "), workingDirectory.getPath(), Util.join(environment, ";"));
    //TODO: correct join env
    NamedPipe client = new NamedPipe(myWinPty.getDataHandle());
    myInputStream = new WinPTYInputStream(client);
    myOutputStream = new WinPTYOutputStream(client);
  }

  @Override
  public boolean isRunning() {
    return myWinPty.exitValue() == -1;
  }

  @Override
  public void setWinSize(WinSize winSize) {
    myWinPty.setWinSize(winSize);
  }

  @Override
  public WinSize getWinSize() throws IOException {
    return null; //TODO
  }

  @Override
  public OutputStream getOutputStream() {
    return myOutputStream;
  }

  @Override
  public InputStream getInputStream() {
    return myInputStream;
  }

  @Override
  public InputStream getErrorStream() {
    return new InputStream() {
      @Override
      public int read() {
        return -1;
      }
    };
  }

  @Override
  public int waitFor() throws InterruptedException {
    for (; ; ) {
      int exitCode = myWinPty.exitValue();
      if (exitCode != -1) {
        return exitCode;
      }
      Thread.sleep(1000);
    }
  }

  @Override
  public int exitValue() {
    int exitValue = myWinPty.exitValue();
    if (exitValue == -1) {
      throw new IllegalThreadStateException("Not terminated yet");

    }
    return exitValue;
  }

  @Override
  public void destroy() {
    myWinPty.close();
  }
}
package com.revolsys.parallel.process;

import org.apache.log4j.Logger;

import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.channel.ChannelDataStore;
import com.revolsys.parallel.channel.ClosedException;
import com.revolsys.parallel.channel.store.Buffer;

public abstract class AbstractInOutProcess<I, O> extends AbstractProcess implements
  InOutProcess<I, O> {

  private Channel<I> in;

  private int inBufferSize = 0;

  private Channel<O> out;

  private int outBufferSize = 0;

  public AbstractInOutProcess() {
  }

  public AbstractInOutProcess(final Channel<I> in, final Channel<O> out) {
    this.in = in;
    this.out = out;
  }

  protected ChannelDataStore<I> createInDataStore() {
    if (this.inBufferSize <= 0) {
      return new Buffer<I>();
    } else {
      return new Buffer<I>(this.inBufferSize);
    }
  }

  protected ChannelDataStore<O> createOutDataStore() {
    if (this.outBufferSize <= 0) {
      return new Buffer<O>();
    } else {
      return new Buffer<O>(this.outBufferSize);
    }
  }

  protected void destroy() {
  }

  /**
   * @return the in
   */
  @Override
  public Channel<I> getIn() {
    if (this.in == null) {
      final String channelName = getBeanName() + ".in";
      final ChannelDataStore<I> buffer = createInDataStore();
      final Channel<I> channel = new Channel<I>(channelName, buffer);
      setIn(channel);
    }
    return this.in;
  }

  public int getInBufferSize() {
    return this.inBufferSize;
  }

  /**
   * @return the out
   */
  @Override
  public Channel<O> getOut() {
    if (this.out == null) {
      final String channelName = getBeanName() + ".out";
      final ChannelDataStore<O> buffer = createOutDataStore();
      final Channel<O> channel = new Channel<O>(channelName, buffer);
      setOut(channel);
    }
    return this.out;
  }

  public int getOutBufferSize() {
    return this.outBufferSize;
  }

  protected void init() {
  }

  @Override
  public final void run() {
    boolean hasError = false;
    final Logger log = Logger.getLogger(getClass());
    try {
      log.debug("Start");
      init();
      run(this.in, this.out);
    } catch (final ClosedException e) {
      log.debug("Shutdown");
    } catch (final ThreadDeath e) {
      log.debug("Shutdown");
    } catch (final Throwable e) {
      log.error(e.getMessage(), e);
      hasError = true;
    } finally {
      if (this.in != null) {
        this.in.readDisconnect();
      }
      if (this.out != null) {
        this.out.writeDisconnect();
      }
      destroy();
    }
    if (hasError) {
      getProcessNetwork().stop();
    }
  }

  protected abstract void run(Channel<I> in, Channel<O> out);

  /**
   * @param in the in to set
   */
  @Override
  public void setIn(final Channel<I> in) {
    this.in = in;
    in.readConnect();
  }

  public void setInBufferSize(final int inBufferSize) {
    this.inBufferSize = inBufferSize;
  }

  /**
   * @param out the out to set
   */
  @Override
  public void setOut(final Channel<O> out) {
    this.out = out;
    out.writeConnect();
  }

  public void setOutBufferSize(final int outBufferSize) {
    this.outBufferSize = outBufferSize;
  }

}

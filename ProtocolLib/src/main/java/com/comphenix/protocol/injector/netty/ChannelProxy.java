package com.comphenix.protocol.injector.netty;

import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.Callable;

import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.google.common.collect.Maps;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

abstract class ChannelProxy implements Channel {
	// Mark that a certain object does not contain a message field
	private static final FieldAccessor MARK_NO_MESSAGE = new FieldAccessor() {
		public void set(Object instance, Object value) {
		}
		public Object get(Object instance) {
			return null;
		}
		public Field getField() {
			return null;
		};
	};

	// Looking up packets in inner classes
	private static Map<Class<?>, FieldAccessor> MESSAGE_LOOKUP = Maps.newConcurrentMap();

	// The underlying channel
	private Channel delegate;
	private Class<?> messageClass;

	// Event loop proxy
	private transient EventLoopProxy loopProxy;

	public ChannelProxy(Channel delegate, Class<?> messageClass) {
		this.delegate = delegate;
		this.messageClass = messageClass;
	}

	/**
	 * Invoked when a packet is scheduled for transmission in the event loop.
	 * 
	 * @param callable - callable to schedule for execution.
	 * @param packetAccessor - accessor for modifying the packet in the
	 *            callable.
	 * @return The callable that will be scheduled, or NULL to cancel.
	 */
	protected abstract <T> Callable<T> onMessageScheduled(Callable<T> callable,
			FieldAccessor packetAccessor);

	/**
	 * Invoked when a packet is scheduled for transmission in the event loop.
	 * 
	 * @param runnable - the runnable that contains a packet to be scheduled.
	 * @param packetAccessor - accessor for modifying the packet in the
	 *            runnable.
	 * @return The runnable that will be scheduled, or NULL to cancel.
	 */
	protected abstract Runnable onMessageScheduled(Runnable runnable, FieldAccessor packetAccessor);

	public <T> Attribute<T> attr(AttributeKey<T> paramAttributeKey) {
		return delegate.attr(paramAttributeKey);
	}

	public ChannelFuture bind(SocketAddress paramSocketAddress) {
		return delegate.bind(paramSocketAddress);
	}

	public ChannelPipeline pipeline() {
		return delegate.pipeline();
	}

	public ChannelFuture connect(SocketAddress paramSocketAddress) {
		return delegate.connect(paramSocketAddress);
	}

	public ByteBufAllocator alloc() {
		return delegate.alloc();
	}

	public ChannelPromise newPromise() {
		return delegate.newPromise();
	}

	public EventLoop eventLoop() {
		if (loopProxy == null) {
			loopProxy = new EventLoopProxy() {
				@Override
				protected EventLoop getDelegate() {
					return delegate.eventLoop();
				}

				@Override
				protected Runnable schedulingRunnable(final Runnable runnable) {
					final FieldAccessor accessor = getMessageAccessor(runnable);

					if (accessor != null) {
						Runnable result = onMessageScheduled(runnable, accessor);;
						return result != null ? result : getEmptyRunnable();
					}
					return runnable;
				}

				@Override
				protected <T> Callable<T> schedulingCallable(Callable<T> callable) {
					FieldAccessor accessor = getMessageAccessor(callable);

					if (accessor != null) {
						Callable<T> result = onMessageScheduled(callable, accessor);;
						return result != null ? result : EventLoopProxy.<T> getEmptyCallable();
					}
					return callable;
				}
			};
		}
		return loopProxy;
	}

	/**
	 * Retrieve a way to access the packet field of an object.
	 * 
	 * @param value - the object.
	 * @return The packet field accessor, or NULL if not found.
	 */
	private FieldAccessor getMessageAccessor(Object value) {
		Class<?> clazz = value.getClass();
		FieldAccessor accessor = MESSAGE_LOOKUP.get(clazz);

		if (accessor == null) {
			try {
				accessor = Accessors.getFieldAccessor(clazz, messageClass, true);
			} catch (IllegalArgumentException e) {
				accessor = MARK_NO_MESSAGE;
			}
			// Save the result
			MESSAGE_LOOKUP.put(clazz, accessor);
		}
		return accessor != MARK_NO_MESSAGE ? accessor : null;
	}

	public ChannelFuture connect(SocketAddress paramSocketAddress1,
			SocketAddress paramSocketAddress2) {
		return delegate.connect(paramSocketAddress1, paramSocketAddress2);
	}

	public ChannelProgressivePromise newProgressivePromise() {
		return delegate.newProgressivePromise();
	}

	public Channel parent() {
		return delegate.parent();
	}

	public ChannelConfig config() {
		return delegate.config();
	}

	public ChannelFuture newSucceededFuture() {
		return delegate.newSucceededFuture();
	}

	public boolean isOpen() {
		return delegate.isOpen();
	}

	public ChannelFuture disconnect() {
		return delegate.disconnect();
	}

	public boolean isRegistered() {
		return delegate.isRegistered();
	}

	public ChannelFuture newFailedFuture(Throwable paramThrowable) {
		return delegate.newFailedFuture(paramThrowable);
	}

	public ChannelFuture close() {
		return delegate.close();
	}

	public boolean isActive() {
		return delegate.isActive();
	}

	@Deprecated
	public ChannelFuture deregister() {
		return delegate.deregister();
	}

	public ChannelPromise voidPromise() {
		return delegate.voidPromise();
	}

	public ChannelMetadata metadata() {
		return delegate.metadata();
	}

	public ChannelFuture bind(SocketAddress paramSocketAddress,
			ChannelPromise paramChannelPromise) {
		return delegate.bind(paramSocketAddress, paramChannelPromise);
	}

	public SocketAddress localAddress() {
		return delegate.localAddress();
	}

	public SocketAddress remoteAddress() {
		return delegate.remoteAddress();
	}

	public ChannelFuture connect(SocketAddress paramSocketAddress,
			ChannelPromise paramChannelPromise) {
		return delegate.connect(paramSocketAddress, paramChannelPromise);
	}

	public ChannelFuture closeFuture() {
		return delegate.closeFuture();
	}

	public boolean isWritable() {
		return delegate.isWritable();
	}

	public Channel flush() {
		return delegate.flush();
	}

	public ChannelFuture connect(SocketAddress paramSocketAddress1,
			SocketAddress paramSocketAddress2, ChannelPromise paramChannelPromise) {
		return delegate.connect(paramSocketAddress1, paramSocketAddress2, paramChannelPromise);
	}

	public Channel read() {
		return delegate.read();
	}

	public Unsafe unsafe() {
		return delegate.unsafe();
	}

	public ChannelFuture disconnect(ChannelPromise paramChannelPromise) {
		return delegate.disconnect(paramChannelPromise);
	}

	public ChannelFuture close(ChannelPromise paramChannelPromise) {
		return delegate.close(paramChannelPromise);
	}

	@Deprecated
	public ChannelFuture deregister(ChannelPromise paramChannelPromise) {
		return delegate.deregister(paramChannelPromise);
	}

	public ChannelFuture write(Object paramObject) {
		return delegate.write(paramObject);
	}

	public ChannelFuture write(Object paramObject, ChannelPromise paramChannelPromise) {
		return delegate.write(paramObject, paramChannelPromise);
	}

	public ChannelFuture writeAndFlush(Object paramObject, ChannelPromise paramChannelPromise) {
		return delegate.writeAndFlush(paramObject, paramChannelPromise);
	}

	public ChannelFuture writeAndFlush(Object paramObject) {
		return delegate.writeAndFlush(paramObject);
	}

	public int compareTo(Channel o) {
		return delegate.compareTo(o);
	}
}

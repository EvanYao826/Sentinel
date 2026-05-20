/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.slotchain;

import com.alibaba.csp.sentinel.context.Context;

/**
 * A wrapper that gives each {@link ProcessorSlotChain} its own independent
 * traversal node, while keeping the underlying {@link ProcessorSlot} instance
 * shared as a singleton.
 * <p>
 * This solves the problem described in
 * <a href="https://github.com/alibaba/Sentinel/issues/3007">issue #3007</a>:
 * singleton slots loaded via SPI have a single {@code next} pointer. When the
 * same slot is added to multiple chains, the last chain built overwrites the
 * {@code next} pointer, causing all previous chains to point to the wrong node.
 * </p>
 * <p>
 * Inspired by Netty's {@code ChannelHandlerContext} pattern: each context node
 * carries its own {@code next} pointer, while the underlying handler (slot)
 * remains shared. Chain traversal goes through context nodes, and the
 * {@link AbstractLinkedProcessorSlot#CHAIN_NEXT_OVERRIDE} ThreadLocal redirects
 * the delegate slot's {@code fireEntry}/{@code fireExit} calls to the next
 * context node instead of the delegate's own {@code next}.
 * </p>
 *
 * @param <T> the type of parameter passed to the slot
 */
public class ProcessorSlotContext<T> extends AbstractLinkedProcessorSlot<T> {

    private final ProcessorSlot<T> delegate;

    public ProcessorSlotContext(ProcessorSlot<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void entry(Context context, ResourceWrapper resourceWrapper, T param, int count, boolean prioritized,
                      Object... args) throws Throwable {
        // Set the chain override so that when the delegate calls fireEntry(),
        // it redirects to this.getNext() (the next wrapper) instead of the
        // delegate's own next pointer.
        CHAIN_NEXT_OVERRIDE.set(this.getNext());
        delegate.entry(context, resourceWrapper, param, count, prioritized, args);
    }

    @Override
    public void exit(Context context, ResourceWrapper resourceWrapper, int count, Object... args) {
        CHAIN_NEXT_OVERRIDE.set(this.getNext());
        delegate.exit(context, resourceWrapper, count, args);
    }

    /**
     * Returns the underlying delegate slot.
     */
    public ProcessorSlot<T> getDelegate() {
        return delegate;
    }
}

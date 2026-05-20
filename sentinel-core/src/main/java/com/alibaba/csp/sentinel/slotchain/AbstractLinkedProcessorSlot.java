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
 * Base class for linked processor slots in a slot chain.
 * <p>
 * Each slot has a {@code next} pointer for chain traversal. When slots are
 * singletons (loaded via SPI), multiple chains sharing the same slot instance
 * will have conflicting {@code next} pointers.
 * </p>
 * <p>
 * To support the {@link ProcessorSlotContext} wrapper pattern, this class
 * provides a {@code CHAIN_NEXT_OVERRIDE} ThreadLocal that allows wrappers
 * to redirect {@code fireEntry}/{@code fireExit} to the next wrapper in
 * the chain, instead of using the delegate slot's own {@code next} pointer.
 * This preserves singleton state while giving each chain independent traversal.
 * </p>
 *
 * @author qinan.qn
 * @author jialiang.linjl
 */
public abstract class AbstractLinkedProcessorSlot<T> implements ProcessorSlot<T> {

    /**
     * ThreadLocal override for chain traversal. When set by a
     * {@link ProcessorSlotContext}, {@code fireEntry}/{@code fireExit} will
     * use this value instead of the instance {@code next} field.
     * <p>
     * This is consumed (removed) on use, so each {@code fireEntry}/{@code fireExit}
     * call uses the override exactly once.
     * </p>
     */
    static final ThreadLocal<AbstractLinkedProcessorSlot<?>> CHAIN_NEXT_OVERRIDE = new ThreadLocal<>();

    private AbstractLinkedProcessorSlot<?> next = null;

    @Override
    public void fireEntry(Context context, ResourceWrapper resourceWrapper, Object obj, int count, boolean prioritized, Object... args)
        throws Throwable {
        AbstractLinkedProcessorSlot<?> chainNext = CHAIN_NEXT_OVERRIDE.get();
        if (chainNext != null) {
            CHAIN_NEXT_OVERRIDE.remove();
            chainNext.transformEntry(context, resourceWrapper, obj, count, prioritized, args);
        } else if (next != null) {
            next.transformEntry(context, resourceWrapper, obj, count, prioritized, args);
        }
    }

    @SuppressWarnings("unchecked")
    void transformEntry(Context context, ResourceWrapper resourceWrapper, Object o, int count, boolean prioritized, Object... args)
        throws Throwable {
        T t = (T)o;
        entry(context, resourceWrapper, t, count, prioritized, args);
    }

    @Override
    public void fireExit(Context context, ResourceWrapper resourceWrapper, int count, Object... args) {
        AbstractLinkedProcessorSlot<?> chainNext = CHAIN_NEXT_OVERRIDE.get();
        if (chainNext != null) {
            CHAIN_NEXT_OVERRIDE.remove();
            chainNext.exit(context, resourceWrapper, count, args);
        } else if (next != null) {
            next.exit(context, resourceWrapper, count, args);
        }
    }

    public AbstractLinkedProcessorSlot<?> getNext() {
        return next;
    }

    public void setNext(AbstractLinkedProcessorSlot<?> next) {
        this.next = next;
    }

}

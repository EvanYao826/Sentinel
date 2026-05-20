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
package com.alibaba.csp.sentinel.slots;

import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.slotchain.AbstractLinkedProcessorSlot;
import com.alibaba.csp.sentinel.slotchain.DefaultProcessorSlotChain;
import com.alibaba.csp.sentinel.slotchain.ProcessorSlot;
import com.alibaba.csp.sentinel.slotchain.ProcessorSlotChain;
import com.alibaba.csp.sentinel.slotchain.ProcessorSlotContext;
import com.alibaba.csp.sentinel.slotchain.SlotChainBuilder;
import com.alibaba.csp.sentinel.spi.Spi;
import com.alibaba.csp.sentinel.spi.SpiLoader;

import java.util.List;

/**
 * Builder for a default {@link ProcessorSlotChain}.
 * <p>
 * Uses the {@link ProcessorSlotContext} wrapper pattern (inspired by Netty's
 * ChannelPipeline) to ensure each chain gets its own independent traversal
 * nodes, while keeping the underlying slot instances as shared singletons.
 * This preserves any per-slot shared state (e.g. cached data) while giving
 * each chain its own {@code next} pointers.
 * </p>
 *
 * @author qinan.qn
 * @author leyou
 * @see ProcessorSlotContext
 */
@Spi(isDefault = true)
public class DefaultSlotChainBuilder implements SlotChainBuilder {

    @Override
    public ProcessorSlotChain build() {
        ProcessorSlotChain chain = new DefaultProcessorSlotChain();

        List<ProcessorSlot> sortedSlotList = SpiLoader.of(ProcessorSlot.class).loadInstanceListSorted();
        for (ProcessorSlot slot : sortedSlotList) {
            if (!(slot instanceof AbstractLinkedProcessorSlot)) {
                RecordLog.warn("The ProcessorSlot(" + slot.getClass().getCanonicalName()
                    + ") is not an instance of AbstractLinkedProcessorSlot, can't be added into ProcessorSlotChain");
                continue;
            }

            // Wrap each slot in a ProcessorSlotContext. The wrapper has its own
            // 'next' pointer, while the underlying slot remains a shared singleton.
            // When the delegate slot calls fireEntry()/fireExit(), the
            // CHAIN_NEXT_OVERRIDE ThreadLocal redirects traversal to the wrapper's
            // next, not the delegate's own next.
            //
            // See: https://github.com/alibaba/Sentinel/issues/3007
            @SuppressWarnings("unchecked")
            ProcessorSlotContext<?> wrapper = new ProcessorSlotContext<>(slot);
            chain.addLast(wrapper);
        }

        return chain;
    }
}

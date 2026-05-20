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

import com.alibaba.csp.sentinel.slotchain.AbstractLinkedProcessorSlot;
import com.alibaba.csp.sentinel.slotchain.ProcessorSlotChain;
import com.alibaba.csp.sentinel.slotchain.ProcessorSlotContext;
import com.alibaba.csp.sentinel.slots.block.authority.AuthoritySlot;
import com.alibaba.csp.sentinel.slots.block.degrade.DefaultCircuitBreakerSlot;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeSlot;
import com.alibaba.csp.sentinel.slots.block.flow.FlowSlot;
import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;
import com.alibaba.csp.sentinel.slots.logger.LogSlot;
import com.alibaba.csp.sentinel.slots.nodeselector.NodeSelectorSlot;
import com.alibaba.csp.sentinel.slots.statistic.StatisticSlot;
import com.alibaba.csp.sentinel.slots.system.SystemSlot;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test cases for {@link DefaultSlotChainBuilder}.
 *
 * @author cdfive
 */
public class DefaultSlotChainBuilderTest {

    @Test
    public void testBuild() {
        DefaultSlotChainBuilder builder = new DefaultSlotChainBuilder();
        ProcessorSlotChain slotChain = builder.build();
        assertNotNull(slotChain);

        // Each slot is wrapped in a ProcessorSlotContext
        AbstractLinkedProcessorSlot<?> next = slotChain.getNext();
        assertTrue(next instanceof ProcessorSlotContext);
        assertTrue(((ProcessorSlotContext<?>) next).getDelegate() instanceof NodeSelectorSlot);

        ProcessorSlotContext<?> wrapper1 = (ProcessorSlotContext<?>) next;

        next = next.getNext();
        assertTrue(next instanceof ProcessorSlotContext);
        assertTrue(((ProcessorSlotContext<?>) next).getDelegate() instanceof ClusterBuilderSlot);

        next = next.getNext();
        assertTrue(next instanceof ProcessorSlotContext);
        assertTrue(((ProcessorSlotContext<?>) next).getDelegate() instanceof LogSlot);

        next = next.getNext();
        assertTrue(next instanceof ProcessorSlotContext);
        assertTrue(((ProcessorSlotContext<?>) next).getDelegate() instanceof StatisticSlot);

        next = next.getNext();
        assertTrue(next instanceof ProcessorSlotContext);
        assertTrue(((ProcessorSlotContext<?>) next).getDelegate() instanceof AuthoritySlot);

        next = next.getNext();
        assertTrue(next instanceof ProcessorSlotContext);
        assertTrue(((ProcessorSlotContext<?>) next).getDelegate() instanceof SystemSlot);

        next = next.getNext();
        assertTrue(next instanceof ProcessorSlotContext);
        assertTrue(((ProcessorSlotContext<?>) next).getDelegate() instanceof FlowSlot);

        next = next.getNext();
        assertTrue(next instanceof ProcessorSlotContext);
        assertTrue(((ProcessorSlotContext<?>) next).getDelegate() instanceof DefaultCircuitBreakerSlot);

        next = next.getNext();
        assertTrue(next instanceof ProcessorSlotContext);
        assertTrue(((ProcessorSlotContext<?>) next).getDelegate() instanceof DegradeSlot);

        next = next.getNext();
        assertNull(next);

        // Build again to verify different chain instances
        ProcessorSlotChain slotChain2 = builder.build();
        assertNotNull(slotChain2);
        assertNotSame(slotChain, slotChain2);

        // Verify wrapper instances are different (each chain has its own wrappers)
        ProcessorSlotContext<?> wrapper2 = (ProcessorSlotContext<?>) slotChain2.getNext();
        assertNotSame(wrapper1, wrapper2);

        // But the underlying delegate slots are the same singleton instances
        assertSame(wrapper1.getDelegate(), wrapper2.getDelegate());
    }
}

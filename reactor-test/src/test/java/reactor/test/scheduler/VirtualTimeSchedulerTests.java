/*
 * Copyright (c) 2011-2017 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.test.scheduler;

import java.util.function.Supplier;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Maldini
 */
public class VirtualTimeSchedulerTests {

	@Test
	public void allEnabled() {
		Assert.assertFalse(Schedulers.newParallel("") instanceof VirtualTimeScheduler);
		Assert.assertFalse(Schedulers.newElastic("") instanceof VirtualTimeScheduler);
		Assert.assertFalse(Schedulers.newSingle("") instanceof VirtualTimeScheduler);

		VirtualTimeScheduler.getOrSet();

		Assert.assertTrue(Schedulers.newParallel("") instanceof VirtualTimeScheduler);
		Assert.assertTrue(Schedulers.newElastic("") instanceof VirtualTimeScheduler);
		Assert.assertTrue(Schedulers.newSingle("") instanceof VirtualTimeScheduler);

		VirtualTimeScheduler t = VirtualTimeScheduler.get();

		Assert.assertSame(Schedulers.newParallel(""), t);
		Assert.assertSame(Schedulers.newElastic(""), t);
		Assert.assertSame(Schedulers.newSingle(""), t);
	}

	@Test
	public void enableProvidedAllSchedulerIdempotent() {
		VirtualTimeScheduler vts = VirtualTimeScheduler.create();

		VirtualTimeScheduler.getOrSet(vts);

		Assert.assertSame(vts, uncache(Schedulers.single()));
		Assert.assertFalse(vts.shutdown);


		VirtualTimeScheduler.getOrSet(vts);

		Assert.assertSame(vts, uncache(Schedulers.single()));
		Assert.assertFalse(vts.shutdown);
	}

	@Test
	public void enableTwoSimilarSchedulersUsesFirst() {
		VirtualTimeScheduler vts1 = VirtualTimeScheduler.create();
		VirtualTimeScheduler vts2 = VirtualTimeScheduler.create();

		VirtualTimeScheduler firstEnableResult = VirtualTimeScheduler.getOrSet(vts1);
		VirtualTimeScheduler secondEnableResult = VirtualTimeScheduler.getOrSet(vts2);

		Assert.assertSame(vts1, firstEnableResult);
		Assert.assertSame(vts1, secondEnableResult);
		Assert.assertSame(vts1, uncache(Schedulers.single()));
		Assert.assertFalse(vts1.shutdown);
	}

	@Test
	public void disposedSchedulerIsStillCleanedUp() {
		VirtualTimeScheduler vts = VirtualTimeScheduler.create();
		vts.dispose();
		assertThat(VirtualTimeScheduler.isFactoryEnabled()).isFalse();

		StepVerifier.withVirtualTime(() -> Mono.just("foo"),
				() -> vts, Long.MAX_VALUE)
	                .then(() -> assertThat(VirtualTimeScheduler.isFactoryEnabled()).isTrue())
	                .then(() -> assertThat(VirtualTimeScheduler.get()).isSameAs(vts))
	                .expectNext("foo")
	                .verifyComplete();

		assertThat(VirtualTimeScheduler.isFactoryEnabled()).isFalse();

		StepVerifier.withVirtualTime(() -> Mono.just("foo"))
	                .then(() -> assertThat(VirtualTimeScheduler.isFactoryEnabled()).isTrue())
	                .then(() -> assertThat(VirtualTimeScheduler.get()).isNotSameAs(vts))
	                .expectNext("foo")
	                .verifyComplete();

		assertThat(VirtualTimeScheduler.isFactoryEnabled()).isFalse();
	}


	@SuppressWarnings("unchecked")
	private static Scheduler uncache(Scheduler potentialCached) {
		if (potentialCached instanceof Supplier) {
			return ((Supplier<Scheduler>) potentialCached).get();
		}
		return potentialCached;
	}

	@After
	public void cleanup() {
		VirtualTimeScheduler.reset();
	}

}
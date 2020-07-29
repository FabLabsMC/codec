/*
 * Copyright 2020 FabLabsMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.fablabsmc.fablabs.api.codec.v1;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

public class ExtraMapCodec<O, A> extends MapCodec<O> {
	private final MapCodec<O> delegate;
	private final MapCodec<A> extra;
	private final BiConsumer<O, A> loader;
	private final Function<O, A> extractor;

	public ExtraMapCodec(MapCodec<O> delegate, MapCodec<A> extra, BiConsumer<O, A> loader, Function<O, A> extractor) {
		this.delegate = delegate;
		this.extra = extra;
		this.loader = loader;
		this.extractor = extractor;
	}

	// to allow consistent keys stream
	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		return Stream.concat(this.delegate.keys(ops), this.extra.keys(ops)); // todo
	}

	@Override
	public <T> DataResult<O> decode(DynamicOps<T> ops, MapLike<T> input) {
		DataResult<O> first = this.delegate.decode(ops, input);
		if (first.result().isPresent()) {
			O parsedFirst = first.result().get();
			DataResult<A> extraResult = this.extra.decode(ops, input);

			if (extraResult.result().isPresent()) {
				this.loader.accept(parsedFirst, extraResult.result().get());
				return DataResult.success(parsedFirst);
			} else {
				return DataResult.error(extraResult.error().get().message(), parsedFirst);
			}
		}

		return first; // erroneous result
	}

	@Override
	public <T> RecordBuilder<T> encode(O input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		RecordBuilder<T> intermediate = this.delegate.encode(input, ops, prefix);
		return this.extra.encode(this.extractor.apply(input), ops, intermediate);
	}
}

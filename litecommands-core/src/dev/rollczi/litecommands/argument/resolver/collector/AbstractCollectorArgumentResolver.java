package dev.rollczi.litecommands.argument.resolver.collector;

import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.SimpleArgument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.parser.Parser;
import dev.rollczi.litecommands.argument.parser.ParserChainAccessor;
import dev.rollczi.litecommands.argument.parser.ParserChained;
import dev.rollczi.litecommands.argument.parser.ParserRegistry;
import dev.rollczi.litecommands.argument.parser.ParserSet;
import dev.rollczi.litecommands.argument.resolver.TypedArgumentResolver;
import dev.rollczi.litecommands.argument.suggester.Suggester;
import dev.rollczi.litecommands.argument.suggester.SuggesterChainAccessor;
import dev.rollczi.litecommands.argument.suggester.SuggesterChained;
import dev.rollczi.litecommands.argument.suggester.SuggesterRegistry;
import dev.rollczi.litecommands.input.raw.RawInput;
import dev.rollczi.litecommands.invalidusage.InvalidUsage;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.range.Range;
import dev.rollczi.litecommands.suggestion.Suggestion;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import dev.rollczi.litecommands.wrapper.WrapFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;

public abstract class AbstractCollectorArgumentResolver<SENDER, E, COLLECTION> extends TypedArgumentResolver<SENDER, COLLECTION, CollectorArgument<COLLECTION>> {

    private final ParserRegistry<SENDER> parserRegistry;
    private final SuggesterRegistry<SENDER> suggesterRegistry;

    public AbstractCollectorArgumentResolver(ParserRegistry<SENDER> parserRegistry, SuggesterRegistry<SENDER> suggesterRegistry) {
        super(CollectorArgument.class);
        this.parserRegistry = parserRegistry;
        this.suggesterRegistry = suggesterRegistry;
    }

    @Override
    public ParseResult<COLLECTION> parseTyped(Invocation<SENDER> invocation, CollectorArgument<COLLECTION> context, RawInput rawInput) {
        return parse(this.getElementType(context, invocation), rawInput, context, invocation);
    }

    private ParseResult<COLLECTION> parse(Class<E> componentType, RawInput rawInput, CollectorArgument<COLLECTION> collectorArgument, Invocation<SENDER> invocation) {
        Argument<E> argument = new SimpleArgument<>(collectorArgument.getKeyName(), WrapFormat.notWrapped(componentType));

        Parser<SENDER, E> parser = parserRegistry.getParser(invocation, argument);

        List<E> values = new ArrayList<>();

        while (rawInput.hasNext()) {
            int count = rawInput.seeAll().size();
            Range range = parser.getRange(argument);

            if (range.isBelowRange(count)) {
                return ParseResult.failure(InvalidUsage.Cause.MISSING_PART_OF_ARGUMENT);
            }

            ParseResult<E> parsedResult = parser.parse(invocation, argument, rawInput);

            if (parsedResult.isFailed()) {
                return ParseResult.failure(parsedResult.getFailedReason());
            }

            values.add(parsedResult.getSuccess());
        }

        Collector<E, ?, ? extends COLLECTION> collector = getCollector(collectorArgument, invocation);
        COLLECTION result = values.stream().collect(collector);

        return ParseResult.success(result);
    }

    abstract Collector<E, ?, ? extends COLLECTION> getCollector(CollectorArgument<COLLECTION> collectorArgument, Invocation<SENDER> invocation);

    @Override
    public Range getTypedRange(CollectorArgument<COLLECTION> argument) {
        return Range.moreThan(0);
    }

    @Override
    public SuggestionResult suggestTyped(Invocation<SENDER> invocation, CollectorArgument<COLLECTION> argument, SuggestionContext context) {
        return suggest(this.getElementType(argument, invocation), context, argument, invocation);
    }

    private <T> SuggestionResult suggest(Class<T> componentType, SuggestionContext context, CollectorArgument<COLLECTION> collectorArgument, Invocation<SENDER> invocation) {
        Argument<T> argument = new SimpleArgument<>(collectorArgument.getKeyName(), WrapFormat.notWrapped(componentType));

        Parser<SENDER, T> parser = parserRegistry.getParser(invocation, argument);
        Suggester<SENDER, T> suggester = suggesterRegistry.getSuggester(componentType, argument.getKey());

        SuggestionResult result = SuggestionResult.empty();

        Suggestion current = context.getCurrent();
        RawInput rawInput = RawInput.of(current.multilevelList());

        while (rawInput.hasNext()) {
            int count = rawInput.seeAll().size();
            Range range = parser.getRange(argument);

            if (range.isInRange(count) || range.isBelowRange(count)) {
                SuggestionContext suggestionContext = new SuggestionContext(Suggestion.from(rawInput.seeAll()));
                int beforeConsumed = suggestionContext.getConsumed();
                SuggestionResult suggestionResult = suggester.suggest(invocation, argument, suggestionContext);

                int afterConsumed = suggestionContext.getConsumed();

                if (afterConsumed >= beforeConsumed) {
                    Suggestion suggestion = current.deleteRight(afterConsumed);
                    return suggestionResult.appendLeft(suggestion.multilevelList());
                }

                rawInput = RawInput.of(suggestionContext.getCurrent().deleteLeft(afterConsumed).multilevelList());
                continue;
            }

            ParseResult<T> parsedResult = parser.parse(invocation, argument, rawInput);

            if (parsedResult.isFailed()) {
                return SuggestionResult.empty();
            }
        }

        return result;
    }

    abstract protected Class<E> getElementType(CollectorArgument<COLLECTION> context, Invocation<SENDER> invocation);

}

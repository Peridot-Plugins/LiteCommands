package dev.rollczi.litecommands.argument.suggester.input;

import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.Parser;
import dev.rollczi.litecommands.argument.parser.ParserChainAccessor;
import dev.rollczi.litecommands.argument.suggester.Suggester;
import dev.rollczi.litecommands.argument.suggester.SuggesterChainAccessor;
import dev.rollczi.litecommands.argument.suggester.SuggesterChained;
import dev.rollczi.litecommands.suggestion.Suggestion;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.input.raw.RawInputAnalyzer;
import dev.rollczi.litecommands.argument.parser.ParserSet;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionResult;

import java.util.Collections;
import java.util.List;

public class SuggestionInputRawImpl implements SuggestionInput<SuggestionInputRawImpl.Matcher> {

    private final List<String> rawArguments;

    SuggestionInputRawImpl(List<String> rawArguments) {
        this.rawArguments = rawArguments;
    }

    @Override
    public Matcher createMatcher() {
        return new Matcher();
    }

    @Override
    public List<String> asList() {
        return Collections.unmodifiableList(this.rawArguments);
    }

    public class Matcher implements SuggestionInputMatcher<Matcher> {

        private final RawInputAnalyzer rawInputAnalyzer = new RawInputAnalyzer(rawArguments);

        public Matcher() {}

        public Matcher(int pivotPosition) {
            this.rawInputAnalyzer.setPivotPosition(pivotPosition);
        }

        @Override
        public boolean hasNextRoute() {
            return rawInputAnalyzer.hasNextRoute();
        }

        @Override
        public boolean nextRouteIsLast() {
            return rawInputAnalyzer.nextRouteIsLast();
        }

        @Override
        public boolean hasNoNextRouteAndArguments() {
            return !rawInputAnalyzer.hasNextRoute();
        }

        @Override
        public String nextRoute() {
            return rawInputAnalyzer.nextRoute();
        }

        @Override
        public String showNextRoute() {
            return rawInputAnalyzer.showNextRoute();
        }

        @Override
        public <SENDER, T> boolean isNextOptional(Invocation<SENDER> invocation, Argument<T> argument, Parser<SENDER, T> parser) {
            return rawInputAnalyzer.isNextOptional(parser, invocation, argument) || argument.hasDefaultValue();
        }

        @Override
        public <SENDER, T> SuggestionInputResult nextArgument(
            Invocation<SENDER> invocation,
            Argument<T> argument,
            Parser<SENDER, T> parser,
            Suggester<SENDER, T> suggester
        ) {
            RawInputAnalyzer.Context<SENDER, T> context = rawInputAnalyzer.toContext(invocation, argument, parser);

            if (context.isMissingFullArgument()) {
                Suggestion current = Suggestion.of(rawInputAnalyzer.getLastArgument());
                SuggestionContext suggestionContext = new SuggestionContext(current);
                SuggestionResult result = suggester.suggest(invocation, argument, suggestionContext)
                    .filterBy(current);

                return SuggestionInputResult.endWith(result);
            }

            if (context.isMissingPartOfArgument()) {
                Suggestion current = Suggestion.from(context.getAllNotConsumedArguments());
                SuggestionContext suggestionContext = new SuggestionContext(current);
                SuggestionResult result = suggester.suggest(invocation, argument, suggestionContext)
                    .filterBy(current);

                return SuggestionInputResult.endWith(result);
            }

            if (context.isLastRawArgument() || context.isPotentialLastArgument()) {
                Suggestion current = Suggestion.from(context.getAllNotConsumedArguments());
                SuggestionContext suggestionContext = new SuggestionContext(current);
                SuggestionResult result = suggester.suggest(invocation, argument, suggestionContext)
                    .filterBy(current);

                int consumed = suggestionContext.getConsumed();
                if (consumed == current.lengthMultilevel()) {
                    return SuggestionInputResult.endWith(result);
                }
            }

            ParseResult<T> result = context.parseArgument(invocation);

            if (result.isFailed()) {
                return SuggestionInputResult.fail();
            }

            return SuggestionInputResult.continueWithout();
        }

        @Override
        public Matcher copy() {
            return new Matcher(rawInputAnalyzer.getPivotPosition());
        }

    }

}

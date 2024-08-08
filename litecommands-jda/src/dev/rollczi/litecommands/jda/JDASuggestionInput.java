package dev.rollczi.litecommands.jda;

import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.Parser;
import dev.rollczi.litecommands.argument.parser.ParserChainAccessor;
import dev.rollczi.litecommands.argument.parser.ParserSet;
import dev.rollczi.litecommands.argument.suggester.SuggesterChainAccessor;
import dev.rollczi.litecommands.argument.suggester.SuggesterChained;
import dev.rollczi.litecommands.argument.suggester.input.SuggestionInputResult;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.argument.suggester.Suggester;
import dev.rollczi.litecommands.suggestion.Suggestion;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import dev.rollczi.litecommands.argument.suggester.input.SuggestionInput;
import dev.rollczi.litecommands.argument.suggester.input.SuggestionInputMatcher;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.List;
import java.util.Map;

class JDASuggestionInput extends AbstractJDAInput<JDASuggestionInput.JDASuggestionMatcher> implements SuggestionInput<JDASuggestionInput.JDASuggestionMatcher> {

    private final AutoCompleteQuery currentOption;

    protected JDASuggestionInput(List<String> routes, Map<String, OptionMapping> arguments, AutoCompleteQuery currentOption) {
        super(routes, arguments);
        this.currentOption = currentOption;
    }

    @Override
    public JDASuggestionMatcher createMatcher() {
        return new JDASuggestionMatcher();
    }

    class JDASuggestionMatcher extends AbstractJDAInput<JDASuggestionMatcher>.AbstractJDAMatcher implements SuggestionInputMatcher<JDASuggestionMatcher> {

        JDASuggestionMatcher() {}

        JDASuggestionMatcher(int routePosition) {
            super(routePosition);
        }

        @Override
        public <SENDER, T> boolean isNextOptional(Invocation<SENDER> invocation, Argument<T> argument, Parser<SENDER, T> parser) {
            return false;
        }

        @Override
        public <SENDER, T> SuggestionInputResult nextArgument(Invocation<SENDER> invocation, Argument<T> argument, Parser<SENDER, T> parser, Suggester<SENDER, T> suggester) {
            if (!argument.getName().equals(currentOption.getName())) {
                return SuggestionInputResult.continueWithout();
            }

            String current = currentOption.getValue();
            Suggestion suggestion = Suggestion.of(current);
            SuggestionContext context = new SuggestionContext(suggestion);
            SuggestionResult result = suggester.suggest(invocation, argument, context);

            return SuggestionInputResult.endWith(result);
        }

        @Override
        public JDASuggestionMatcher copy() {
            return new JDASuggestionMatcher(routePosition);
        }

        @Override
        public boolean nextRouteIsLast() {
            return false; // don't suggest routes
        }

        @Override
        public boolean hasNoNextRouteAndArguments() {
            return !hasNextRoute() && arguments.isEmpty();
        }

    }

}

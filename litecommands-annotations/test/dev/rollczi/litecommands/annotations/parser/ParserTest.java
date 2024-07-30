package dev.rollczi.litecommands.annotations.parser;

import dev.rollczi.litecommands.annotations.LiteConfig;
import dev.rollczi.litecommands.annotations.LiteTestSpec;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.parser.Parser;
import dev.rollczi.litecommands.argument.parser.ParserChainAccessor;
import dev.rollczi.litecommands.argument.parser.ParserChained;
import dev.rollczi.litecommands.input.raw.RawInput;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.range.Range;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParserTest extends LiteTestSpec {

    static LiteConfig config = builder -> builder
        .argumentParser(User.class, new UserParser<>())
        .argumentParser(Guild.class, new GuildParser<>());

    static Map<String, User> USERS = new ConcurrentHashMap<>();

    static class User {

        private final String name;
        private Guild guild;

        User(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public @Nullable Guild getGuild() {
            return this.guild;
        }

        public void setGuild(Guild guild) {
            this.guild = guild;
        }

    }

    static class Guild {

        private final String name;

        Guild(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

    }

    static class UserParser<S> implements Parser<S, User> {

        @Override
        public ParseResult<User> parse(Invocation<S> invocation, Argument<User> argument, RawInput input) {
            return ParseResult.success(USERS.computeIfAbsent(invocation.name(), name -> new User(name)));
        }

        @Override
        public Range getRange(Argument<User> userArgument) {
            return Range.ONE;
        }

    }

    static class GuildParser<S> implements ParserChained<S, Guild> {

        @Override
        public ParseResult<Guild> parse(Invocation<S> invocation, Argument<Guild> argument, RawInput input, ParserChainAccessor<S> chainAccessor) {
            return chainAccessor.parse(invocation, Argument.of(argument, User.class), input)
                .flatMap(user -> user.getGuild() == null
                    ? ParseResult.failure("User is not in a guild")
                    : ParseResult.success(user.getGuild())
                );
        }

        @Override
        public Range getRange(Argument<Guild> guildArgument) {
            return Range.ONE;
        }

    }

    @Command(name = "command")
    static class TestCommand {

        @Execute(name = "user")
        void execute(@Arg("user") User user) {}

        @Execute(name = "guild get")
        void execute(@Arg Guild guild) {}

        @Execute(name = "guild set")
        void execute(@Arg User user, @Arg String guildName) {
            user.setGuild(new Guild(guildName));
            USERS.put(user.getName(), user);
        }

        @Execute(name = "failed")
        void execute(@Arg Integer integer) {}

    }

    @Test
    @DisplayName("Should provide user context")
    void testUserContext() {
        platform.execute("command user \"test-user\"")
            .assertSuccess();
    }

}

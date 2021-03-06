package cc.whohow.db;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Predicates {
    public static Predicate<JsonNode> include(String key, String include) {
        if (include == null || include.isEmpty()) {
            return Predicates::all;
        }
        return include(key, asSet(include));
    }

    public static Predicate<JsonNode> include(String key, Collection<String> include) {
        return new JsonPredicate(key, include::contains);
    }

    public static Predicate<JsonNode> exclude(String key, String exclude) {
        return include(key, exclude).negate();
    }

    public static Predicate<JsonNode> exclude(String key, Collection<String> exclude) {
        return include(key, exclude).negate();
    }

    public static Predicate<JsonNode> pattern(String key, String pattern) {
        return pattern(key, Pattern.compile(pattern));
    }

    public static Predicate<JsonNode> pattern(String key, Pattern pattern) {
        return new JsonPredicate(key, pattern.asPredicate());
    }

    private static Set<String> asSet(String csv) {
        return Pattern.compile(",")
                .splitAsStream(csv)
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    public static <T> boolean all(T o) {
        return true;
    }

    public static <T1, T2> boolean all(T1 o1, T2 o2) {
        return true;
    }

    private static class JsonPredicate implements Predicate<JsonNode> {
        private final String key;
        private final Predicate<String> predicate;

        private JsonPredicate(String key, Predicate<String> predicate) {
            this.key = key;
            this.predicate = predicate;
        }

        @Override
        public boolean test(JsonNode node) {
            if (key != null) {
                if (key.startsWith("/")) {
                    return predicate.test(node.at(key).asText());
                } else {
                    return predicate.test(node.path(key).asText());
                }
            }
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(node.fields(), 0), false)
                    .map(Map.Entry::getValue)
                    .map(json -> json.asText(""))
                    .anyMatch(predicate);
        }
    }
}

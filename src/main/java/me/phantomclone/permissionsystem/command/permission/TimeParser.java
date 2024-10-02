package me.phantomclone.permissionsystem.command.permission;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class TimeParser {

  private static final Pattern validTimePattern = Pattern.compile("^(\\d+[yMdhm])+$");
  private static final Pattern extractTimePattern = Pattern.compile("(\\d+)([yMdhm])");
  private static final Pattern suggestionPattern = Pattern.compile("(\\d+)([yMdhm])?");
  private static final List<String> UNITS = List.of("y", "M", "d", "h", "m");

  private final String identifier;
  private final BiFunction<LocalDateTime, Integer, LocalDateTime> applyFunction;
  private Integer value = -1;

  private LocalDateTime apply(LocalDateTime localDateTime) {
    if (value == -1) {
      return localDateTime;
    }

    return applyFunction.apply(localDateTime, value);
  }

  static LocalDateTime parsePattern(String input) {
    Matcher validMatcher = validTimePattern.matcher(input);
    if (!validMatcher.matches()) {
      throw new IllegalArgumentException();
    }

    Matcher matcher = extractTimePattern.matcher(input);

    List<TimeParser> timeParsers =
        List.of(
            new TimeParser("y", LocalDateTime::plusYears),
            new TimeParser("M", LocalDateTime::plusMonths),
            new TimeParser("d", LocalDateTime::plusDays),
            new TimeParser("h", LocalDateTime::plusHours),
            new TimeParser("m", LocalDateTime::plusMinutes));

    while (matcher.find()) {
      int value = Integer.parseInt(matcher.group(1));
      String unit = matcher.group(2);

      timeParsers.stream()
              .filter(timeParser -> timeParser.identifier.equals(unit))
              .filter(timeParser -> timeParser.value == -1)
              .findFirst()
              .orElseThrow()
              .value =
          value;
    }

    LocalDateTime localDateTime = LocalDateTime.now();
    for (TimeParser timeParser : timeParsers) {
      localDateTime = timeParser.apply(localDateTime);
    }

    return localDateTime;
  }

  static List<String> suggestCompletions(String input) {
    if (input.isEmpty()) {
      return List.of("1y1M1d1h1m", "1y10d", "50m");
    }

    List<String> suggestions = new ArrayList<>();
    Matcher matcher = suggestionPattern.matcher(input);

    Set<String> usedUnits = new HashSet<>();
    String lastNumber = "";
    String lastUnit = null;

    while (matcher.find()) {
      lastNumber = matcher.group(1);
      lastUnit = matcher.group(2);
      if (lastUnit != null) {
        usedUnits.add(lastUnit);
      }
    }

    if ((lastUnit == null && !lastNumber.isEmpty())
        || (lastUnit != null && !lastNumber.isEmpty())) {
      UNITS.stream()
          .filter(unit -> !usedUnits.contains(unit))
          .map(unit -> "%s%s".formatted(input, unit))
          .forEach(suggestions::add);
    }

    return suggestions;
  }
}

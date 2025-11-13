package nz.co.jammehcow.jenkinsdiscord;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.PatternSyntaxException;

/**
 * @author Marek Hasselder
 */
public class DynamicFieldContainer {

  private final List<DynamicField> fields = new ArrayList<>();

  public List<DynamicField> getFields() {
    return fields;
  }

  public DynamicFieldContainer addField(String key, String value) {
    fields.add(new DynamicField(key, value));
    return this;
  }

  @Override
  public String toString() {
    // Could be optimized using >8 Java Features
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < fields.size(); i++) {
      // Serialize the field using the custom toString method
      builder.append(fields.get(i).toString());
      if (i + 1 < fields.size()) {
        builder.append(", ");
      }
    }
    return builder.toString();
  }

  /**
   * Creates a new dynamic field container using the fieldsString.
   *
   * @param fieldsString the string containing the dynamic fields (key1:value1, key2:value2)
   * @return a dynamic field container containing the
   * @throws RuntimeException if the string is in a wrong format
   */
  public static DynamicFieldContainer of(String fieldsString) {
    DynamicFieldContainer fieldContainer = new DynamicFieldContainer();

    if(fieldsString.length() == 0) {
        return fieldContainer;
    }

    try {
      for (String s : fieldsString.split(",\\s*")) {
        String[] pair = s.split(":", 2);
        fieldContainer.addField(pair[0], pair[1]);
      }
    } catch (PatternSyntaxException e) {
      throw new RuntimeException("Dynamic fields string is in a wrong format. Using empty container as fallback", e);
    }

    return fieldContainer;
  }

  public static class DynamicField {

    private final String key;
    private final String value;

    public DynamicField(String key, String value) {
      Objects.requireNonNull(key);
      Objects.requireNonNull(value);
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.format("%s:%s", key, value);
    }
  }


}

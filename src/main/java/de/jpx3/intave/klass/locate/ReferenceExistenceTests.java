package de.jpx3.intave.klass.locate;

import de.jpx3.intave.IntaveLogger;
import de.jpx3.intave.test.IntegrationTests;
import de.jpx3.intave.test.Severity;
import de.jpx3.intave.test.Test;

public final class ReferenceExistenceTests extends IntegrationTests {
  public ReferenceExistenceTests() {
    super("RE");
  }

  @Test(
    testCode = "A",
    severity = Severity.ERROR
  )
  public void checkClasses() {
    for (ClassLocation classLocation : Locate.classLocations()) {
      try {
        String className = classLocation.access().getName();
      } catch (Exception exception) {
        IntaveLogger.logger().warn("类 " + classLocation.compiledLocation() + " 不存在");
//        exception.printStackTrace();
        throw exception;
      }
    }
  }

  @Test(
    testCode = "B",
    severity = Severity.ERROR
  )
  public void checkMethods() {
    for (MethodLocation methodLocation : Locate.methodLocations()) {
      try {
        methodLocation.access();
      } catch (Exception exception) {
        IntaveLogger.logger().warn("类 " + methodLocation.classKey() + " 中的方法 " + methodLocation.methodNameOfKey() + "@" + methodLocation.targetMethodName() + methodLocation.targetMethodSignature() + " 不存在");
//        exception.printStackTrace();
        throw exception;
      }
    }

  }

  @Test(
    testCode = "C",
    severity = Severity.ERROR
  )
  public void checkFields() {
    for (FieldLocation fieldLocation : Locate.fieldLocations()) {
      try {
        fieldLocation.access();
      } catch (Exception exception) {
        IntaveLogger.logger().warn("类 " + fieldLocation.classKey() + " 中的字段 " + fieldLocation.key() + "/" + fieldLocation.targetName() + " 不存在");
        throw exception;
      }
    }
  }
}

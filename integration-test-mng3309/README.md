<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# MNG-3309 Cascading Profile Activation Integration Test

This integration test verifies the cascading profile activation functionality implemented for [MNG-3309](https://issues.apache.org/jira/browse/MNG-3309).

## What is Cascading Profile Activation?

Cascading profile activation allows profiles to activate other profiles through property injection. When a profile is activated, its properties are injected into the model, and these properties can then trigger the activation of other profiles in subsequent iterations.

## Test Scenarios

### 1. Basic Cascading Activation

```bash
mvn validate -Dtrigger=start -Dmaven.profile.activation.cascading=true
```

**Expected behavior:**
- `profile1` is activated by `trigger=start`
- `profile1` sets `level1.cascade=activate`
- `profile2` is activated by `level1.cascade=activate`
- `profile2` sets `level2.cascade=activate`
- `profile3` is activated by `level2.cascade=activate`
- `profile4` is NOT activated (requires `level3.cascade=different`)

**Expected output:**
```
Active profiles:
 - profile1 (source: pom)
 - profile2 (source: pom)
 - profile3 (source: pom)

Test Result: profile3-activated
Profile1 Active: true
Profile2 Active: true
Profile3 Active: true
Profile4 Active: false
```

### 2. Circular Dependency Handling

```bash
mvn validate -Dcircular=test -Dmaven.profile.activation.cascading=true
```

**Expected behavior:**
- `circular` profile is activated by `circular=test`
- `circular` profile sets `trigger=start`
- `profile1` is activated by `trigger=start` (from circular profile)
- Cascading continues as in scenario 1
- No infinite loop occurs

**Expected output:**
```
Active profiles:
 - circular (source: pom)
 - profile1 (source: pom)
 - profile2 (source: pom)
 - profile3 (source: pom)
```

### 3. Stop Condition

```bash
mvn validate -Dstop.test=true -Dmaven.profile.activation.cascading=true
```

**Expected behavior:**
- `stop-test` profile is activated by `stop.test=true`
- `stop-test` profile sets `stop.cascade=nowhere`
- No other profiles are activated (no profile matches `stop.cascade=nowhere`)
- Cascading stops after first iteration

**Expected output:**
```
Active profiles:
 - stop-test (source: pom)

Test Result: stop-activated
Stop Active: true
```

### 4. No Cascading (Baseline)

```bash
mvn validate
```

**Expected behavior:**
- No profiles are activated
- No cascading occurs

**Expected output:**
```
Active profiles: (none)

Test Result: none
```

## Test Implementation Details

### Profile Chain
1. **profile1**: Activated by `trigger=start` → sets `level1.cascade=activate`
2. **profile2**: Activated by `level1.cascade=activate` → sets `level2.cascade=activate`
3. **profile3**: Activated by `level2.cascade=activate` → sets `level3.cascade=activate`
4. **profile4**: Activated by `level3.cascade=different` (never triggered)

### Circular Dependency Test
- **circular**: Activated by `circular=test` → sets `trigger=start`
- This creates a circular dependency with **profile1**
- Tests that the implementation handles cycles gracefully

### Stop Condition Test
- **stop-test**: Activated by `stop.test=true` → sets `stop.cascade=nowhere`
- No other profile matches this property, so cascading stops

## Record Immutability Test

The test also verifies that Maven 4's immutable record-based profiles work correctly with cascading activation:

- Profile records remain immutable during the cascading process
- Property injection creates new context states without modifying original profiles
- The cascading mechanism works with the new API design

## Running the Tests

1. Build Maven 4 with the cascading profile activation implementation
2. Navigate to this test directory
3. Run the test scenarios above
4. Verify the output matches the expected behavior

## Verification

The test uses:
- **maven-help-plugin** to display active profiles
- **maven-antrun-plugin** to display property values
- Property tracking to verify which profiles were activated

Success criteria:
- ✅ Correct profiles are activated in cascading scenarios
- ✅ Circular dependencies don't cause infinite loops
- ✅ Cascading stops when no more profiles can be activated
- ✅ Profile records remain immutable
- ✅ Property injection works correctly with the new API

## Implementation Notes

This test validates the implementation of:
- `ProfileActivationContext.addProfileProperties()` method
- `DefaultProfileSelector.getActiveProfiles()` with cascading behavior
- Iterative profile activation until no new profiles are activated
- Proper handling of Maven 4's immutable model design

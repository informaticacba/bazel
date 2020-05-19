// Copyright 2015 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.analysis;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration;
import com.google.devtools.build.lib.analysis.util.AnalysisTestCase;
import com.google.devtools.build.lib.analysis.util.TestAspects;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.packages.AspectDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link Dependency}.
 *
 * <p>Although this is just a data class, we need a way to create a configuration.
 */
@RunWith(JUnit4.class)
public class DependencyTest extends AnalysisTestCase {
  @Test
  public void withNullConfiguration_BasicAccessors() throws Exception {
    Dependency nullDep =
        Dependency.withNullConfiguration(Label.parseAbsolute("//a", ImmutableMap.of()));

    assertThat(nullDep.getLabel()).isEqualTo(Label.parseAbsolute("//a", ImmutableMap.of()));
    assertThat(nullDep.getConfiguration()).isNull();
    assertThat(nullDep.getAspects().getAllAspects()).isEmpty();
  }

  @Test
  public void withConfiguration_BasicAccessors() throws Exception {
    update();
    Dependency targetDep =
        Dependency.withConfiguration(
            Label.parseAbsolute("//a", ImmutableMap.of()), getTargetConfiguration());

    assertThat(targetDep.getLabel()).isEqualTo(Label.parseAbsolute("//a", ImmutableMap.of()));
    assertThat(targetDep.getConfiguration()).isEqualTo(getTargetConfiguration());
    assertThat(targetDep.getAspects().getAllAspects()).isEmpty();
  }

  @Test
  public void withConfigurationAndAspects_BasicAccessors() throws Exception {
    update();
    AspectDescriptor simpleAspect = new AspectDescriptor(TestAspects.SIMPLE_ASPECT);
    AspectDescriptor attributeAspect = new AspectDescriptor(TestAspects.ATTRIBUTE_ASPECT);
    AspectCollection twoAspects = AspectCollection.createForTests(
        ImmutableSet.of(simpleAspect, attributeAspect));
    Dependency targetDep =
        Dependency.withConfigurationAndAspects(
            Label.parseAbsolute("//a", ImmutableMap.of()), getTargetConfiguration(), twoAspects);

    assertThat(targetDep.getLabel()).isEqualTo(Label.parseAbsolute("//a", ImmutableMap.of()));
    assertThat(targetDep.getConfiguration()).isEqualTo(getTargetConfiguration());
    assertThat(targetDep.getAspects()).isEqualTo(twoAspects);
    assertThat(targetDep.getAspectConfiguration(simpleAspect)).isEqualTo(getTargetConfiguration());
    assertThat(targetDep.getAspectConfiguration(attributeAspect))
        .isEqualTo(getTargetConfiguration());
  }

  @Test
  public void withConfigurationAndAspects_RejectsNullConfigWithNPE() throws Exception {
    // Although the NullPointerTester should check this, this test invokes a different code path,
    // because it includes aspects (which the NPT test will not).
    AspectDescriptor simpleAspect = new AspectDescriptor(TestAspects.SIMPLE_ASPECT);
    AspectDescriptor attributeAspect = new AspectDescriptor(TestAspects.ATTRIBUTE_ASPECT);
    AspectCollection twoAspects = AspectCollection.createForTests(simpleAspect, attributeAspect);

    assertThrows(
        NullPointerException.class,
        () ->
            Dependency.withConfigurationAndAspects(
                Label.parseAbsolute("//a", ImmutableMap.of()), null, twoAspects));
  }

  @Test
  public void withConfigurationAndAspects_AllowsEmptyAspectSet() throws Exception {
    update();
    Dependency dep =
        Dependency.withConfigurationAndAspects(
            Label.parseAbsolute("//a", ImmutableMap.of()),
            getTargetConfiguration(),
            AspectCollection.EMPTY);
    // Here we're also checking that this doesn't throw an exception. No boom? OK. Good.
    assertThat(dep.getAspects().getAllAspects()).isEmpty();
  }

  @Test
  public void withConfiguredAspects_BasicAccessors() throws Exception {
    update();
    AspectDescriptor simpleAspect = new AspectDescriptor(TestAspects.SIMPLE_ASPECT);
    AspectDescriptor attributeAspect = new AspectDescriptor(TestAspects.ATTRIBUTE_ASPECT);
    AspectCollection aspects =
        AspectCollection.createForTests(ImmutableSet.of(simpleAspect, attributeAspect));
    ImmutableMap<AspectDescriptor, BuildConfiguration> twoAspectMap = ImmutableMap.of(
        simpleAspect, getTargetConfiguration(), attributeAspect, getHostConfiguration());
    Dependency targetDep =
        Dependency.withConfiguredAspects(
            Label.parseAbsolute("//a", ImmutableMap.of()),
            getTargetConfiguration(),
            aspects,
            twoAspectMap);

    assertThat(targetDep.getLabel()).isEqualTo(Label.parseAbsolute("//a", ImmutableMap.of()));
    assertThat(targetDep.getConfiguration()).isEqualTo(getTargetConfiguration());
    assertThat(targetDep.getAspects().getAllAspects())
        .containsExactly(simpleAspect, attributeAspect);
    assertThat(targetDep.getAspectConfiguration(simpleAspect)).isEqualTo(getTargetConfiguration());
    assertThat(targetDep.getAspectConfiguration(attributeAspect))
        .isEqualTo(getHostConfiguration());
  }


  @Test
  public void withConfiguredAspects_AllowsEmptyAspectMap() throws Exception {
    update();
    Dependency dep =
        Dependency.withConfiguredAspects(
            Label.parseAbsolute("//a", ImmutableMap.of()),
            getTargetConfiguration(),
            AspectCollection.EMPTY,
            ImmutableMap.<AspectDescriptor, BuildConfiguration>of());
    // Here we're also checking that this doesn't throw an exception. No boom? OK. Good.
    assertThat(dep.getAspects().getAllAspects()).isEmpty();
  }

  @Test
  public void factoriesPassNullableTester() throws Exception {
    update();

    new NullPointerTester()
        .setDefault(Label.class, Label.parseAbsolute("//a", ImmutableMap.of()))
        .setDefault(BuildConfiguration.class, getTargetConfiguration())
        .testAllPublicStaticMethods(Dependency.class);
  }

  @Test
  public void equalsPassesEqualsTester() throws Exception {
    update();

    Label a = Label.parseAbsolute("//a", ImmutableMap.of());
    Label aExplicit = Label.parseAbsolute("//a:a", ImmutableMap.of());
    Label b = Label.parseAbsolute("//b", ImmutableMap.of());

    BuildConfiguration host = getHostConfiguration();
    BuildConfiguration target = getTargetConfiguration();

    AspectDescriptor simpleAspect = new AspectDescriptor(TestAspects.SIMPLE_ASPECT);
    AspectDescriptor attributeAspect = new AspectDescriptor(TestAspects.ATTRIBUTE_ASPECT);
    AspectDescriptor errorAspect = new AspectDescriptor(TestAspects.ERROR_ASPECT);

    AspectCollection twoAspects =
        AspectCollection.createForTests(simpleAspect, attributeAspect);
    AspectCollection inverseAspects =
        AspectCollection.createForTests(attributeAspect, simpleAspect);
    AspectCollection differentAspects =
        AspectCollection.createForTests(attributeAspect, errorAspect);
    AspectCollection noAspects = AspectCollection.EMPTY;

    ImmutableMap<AspectDescriptor, BuildConfiguration> twoAspectsHostMap =
        ImmutableMap.of(simpleAspect, host, attributeAspect, host);
    ImmutableMap<AspectDescriptor, BuildConfiguration> twoAspectsTargetMap =
        ImmutableMap.of(simpleAspect, target, attributeAspect, target);
    ImmutableMap<AspectDescriptor, BuildConfiguration> differentAspectsHostMap =
        ImmutableMap.of(attributeAspect, host, errorAspect, host);
    ImmutableMap<AspectDescriptor, BuildConfiguration> differentAspectsTargetMap =
        ImmutableMap.of(attributeAspect, target, errorAspect, target);
    ImmutableMap<AspectDescriptor, BuildConfiguration> noAspectsMap =
        ImmutableMap.<AspectDescriptor, BuildConfiguration>of();

    new EqualsTester()
        .addEqualityGroup(
            // base set: //a, host configuration, normal aspect set
            Dependency.withConfigurationAndAspects(a, host, twoAspects),
            Dependency.withConfigurationAndAspects(aExplicit, host, twoAspects),
            Dependency.withConfigurationAndAspects(a, host, inverseAspects),
            Dependency.withConfigurationAndAspects(aExplicit, host, inverseAspects),
            Dependency.withConfiguredAspects(a, host, twoAspects, twoAspectsHostMap),
            Dependency.withConfiguredAspects(aExplicit, host, twoAspects, twoAspectsHostMap))
        .addEqualityGroup(
            // base set but with label //b
            Dependency.withConfigurationAndAspects(b, host, twoAspects),
            Dependency.withConfigurationAndAspects(b, host, inverseAspects),
            Dependency.withConfiguredAspects(b, host, twoAspects, twoAspectsHostMap))
        .addEqualityGroup(
            // base set but with target configuration
            Dependency.withConfigurationAndAspects(a, target, twoAspects),
            Dependency.withConfigurationAndAspects(aExplicit, target, twoAspects),
            Dependency.withConfigurationAndAspects(a, target, inverseAspects),
            Dependency.withConfigurationAndAspects(aExplicit, target, inverseAspects),
            Dependency.withConfiguredAspects(a, target, twoAspects, twoAspectsTargetMap),
            Dependency.withConfiguredAspects(aExplicit, target, twoAspects, twoAspectsTargetMap))
        .addEqualityGroup(
            // base set but with null configuration
            Dependency.withNullConfiguration(a),
            Dependency.withNullConfiguration(aExplicit))
        .addEqualityGroup(
            // base set but with different aspects
            Dependency.withConfigurationAndAspects(a, host, differentAspects),
            Dependency.withConfigurationAndAspects(aExplicit, host, differentAspects),
            Dependency.withConfiguredAspects(
                a, host, differentAspects, differentAspectsHostMap),
            Dependency.withConfiguredAspects(
                aExplicit, host, differentAspects, differentAspectsHostMap))
        .addEqualityGroup(
            // base set but with label //b and target configuration
            Dependency.withConfigurationAndAspects(b, target, twoAspects),
            Dependency.withConfigurationAndAspects(b, target, inverseAspects),
            Dependency.withConfiguredAspects(b, target,
                twoAspects, twoAspectsTargetMap))
        .addEqualityGroup(
            // base set but with label //b and null configuration
            Dependency.withNullConfiguration(b))
        .addEqualityGroup(
            // base set but with label //b and different aspects
            Dependency.withConfigurationAndAspects(b, host, differentAspects),
            Dependency.withConfiguredAspects(
                b, host, differentAspects, differentAspectsHostMap))
        .addEqualityGroup(
            // base set but with target configuration and different aspects
            Dependency.withConfigurationAndAspects(a, target, differentAspects),
            Dependency.withConfigurationAndAspects(aExplicit, target, differentAspects),
            Dependency.withConfiguredAspects(
                a, target, differentAspects, differentAspectsTargetMap),
            Dependency.withConfiguredAspects(
                aExplicit, target, differentAspects, differentAspectsTargetMap))
        .addEqualityGroup(
            // inverse of base set: //b, target configuration, different aspects
            Dependency.withConfigurationAndAspects(b, target, differentAspects),
            Dependency.withConfiguredAspects(
                b, target, differentAspects, differentAspectsTargetMap))
        .addEqualityGroup(
            // base set but with no aspects
            Dependency.withConfiguration(a, host),
            Dependency.withConfiguration(aExplicit, host),
            Dependency.withConfigurationAndAspects(a, host, noAspects),
            Dependency.withConfigurationAndAspects(aExplicit, host, noAspects),
            Dependency.withConfiguredAspects(a, host, noAspects, noAspectsMap),
            Dependency.withConfiguredAspects(aExplicit, host, noAspects, noAspectsMap))
        .addEqualityGroup(
            // base set but with label //b and no aspects
            Dependency.withConfiguration(b, host),
            Dependency.withConfigurationAndAspects(b, host, noAspects),
            Dependency.withConfiguredAspects(b, host, noAspects, noAspectsMap))
        .addEqualityGroup(
            // base set but with target configuration and no aspects
            Dependency.withConfiguration(a, target),
            Dependency.withConfiguration(aExplicit, target),
            Dependency.withConfigurationAndAspects(a, target, noAspects),
            Dependency.withConfigurationAndAspects(aExplicit, target, noAspects),
            Dependency.withConfiguredAspects(a, target, noAspects, noAspectsMap),
            Dependency.withConfiguredAspects(aExplicit, target, noAspects, noAspectsMap))
        .addEqualityGroup(
            // inverse of base set: //b, target configuration, no aspects
            Dependency.withConfiguration(b, target),
            Dependency.withConfigurationAndAspects(b, target, noAspects),
            Dependency.withConfiguredAspects(b, target, noAspects, noAspectsMap))
        .testEquals();
  }
}

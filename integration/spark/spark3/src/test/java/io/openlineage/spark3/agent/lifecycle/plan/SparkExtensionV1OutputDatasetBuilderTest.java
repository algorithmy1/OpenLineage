/*
/* Copyright 2018-2024 contributors to the OpenLineage project
/* SPDX-License-Identifier: Apache-2.0
*/

package io.openlineage.spark3.agent.lifecycle.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.openlineage.client.OpenLineage;
import io.openlineage.client.OpenLineage.OutputDataset;
import io.openlineage.client.utils.DatasetIdentifier;
import io.openlineage.spark.agent.Versions;
import io.openlineage.spark.api.OpenLineageContext;
import io.openlineage.spark.extension.scala.v1.OutputDatasetWithDelegate;
import io.openlineage.spark.extension.scala.v1.OutputDatasetWithFacets;
import io.openlineage.spark.extension.scala.v1.OutputDatasetWithIdentifier;
import io.openlineage.spark.extension.scala.v1.OutputLineageNode;
import java.util.Arrays;
import java.util.Collections;
import lombok.SneakyThrows;
import org.apache.spark.scheduler.SparkListenerEvent;
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scala.collection.JavaConverters;

class SparkExtensionV1OutputDatasetBuilderTest {

  OpenLineage openLineage = new OpenLineage(Versions.OPEN_LINEAGE_PRODUCER_URI);
  OpenLineageContext context = mock(OpenLineageContext.class);
  SparkExtensionV1OutputDatasetBuilder builder;
  LogicalPlan plan =
      mock(LogicalPlan.class, withSettings().extraInterfaces(OutputLineageNode.class));
  SparkListenerEvent event = mock(SparkListenerEvent.class);

  @BeforeEach
  @SneakyThrows
  public void setUp() {
    builder = mock(SparkExtensionV1OutputDatasetBuilder.class);

    when(context.getOpenLineage()).thenReturn(openLineage);
    when(builder.getContext()).thenReturn(context);
    when(builder.apply(any(), any())).thenCallRealMethod();
    when(builder.isDefinedAtLogicalPlan(any())).thenCallRealMethod();
  }

  @Test
  void testIsDefinedAt() {
    assertThat(builder.isDefinedAtLogicalPlan(mock(LogicalPlan.class))).isFalse();
    assertThat(builder.isDefinedAtLogicalPlan(plan)).isTrue();
  }

  @Test
  void testExtractDatasetWithIdentifier() {
    when(((OutputLineageNode) plan).getOutputs(any()))
        .thenReturn(
            JavaConverters.asScalaBuffer(
                    Collections.<OutputDatasetWithFacets>singletonList(
                        new OutputDatasetWithIdentifier(
                            new DatasetIdentifier("a", "b"),
                            openLineage.newDatasetFacetsBuilder(),
                            openLineage.newOutputDatasetOutputFacetsBuilder())))
                .toList());

    assertThat(builder.apply(event, plan)).hasSize(1);
    assertThat(builder.apply(event, plan).get(0))
        .hasFieldOrPropertyWithValue("name", "a")
        .hasFieldOrPropertyWithValue("namespace", "b");
  }

  @Test
  void testExtractDatasetWithDelegate() {
    LogicalPlan delegate = mock(LogicalPlan.class);
    OutputDataset dataset = mock(OutputDataset.class);
    when(builder.delegate(event, delegate)).thenReturn(Arrays.asList(dataset));

    when(((OutputLineageNode) plan).getOutputs(any()))
        .thenReturn(
            JavaConverters.asScalaBuffer(
                    Collections.<OutputDatasetWithFacets>singletonList(
                        new OutputDatasetWithDelegate(
                            delegate,
                            openLineage.newDatasetFacetsBuilder(),
                            openLineage.newOutputDatasetOutputFacetsBuilder())))
                .toList());

    assertThat(builder.apply(event, plan)).containsExactly(dataset);
  }

  @Test
  void testConcatAllDatasets() {
    LogicalPlan delegate = mock(LogicalPlan.class);
    OutputDataset dataset = mock(OutputDataset.class);
    when(builder.delegate(event, delegate)).thenReturn(Arrays.asList(dataset));

    when(((OutputLineageNode) plan).getOutputs(any()))
        .thenReturn(
            JavaConverters.asScalaBuffer(
                    Arrays.asList(
                        (OutputDatasetWithFacets)
                            new OutputDatasetWithDelegate(
                                delegate,
                                openLineage.newDatasetFacetsBuilder(),
                                openLineage.newOutputDatasetOutputFacetsBuilder()),
                        (OutputDatasetWithFacets)
                            new OutputDatasetWithDelegate(
                                delegate,
                                openLineage.newDatasetFacetsBuilder(),
                                openLineage.newOutputDatasetOutputFacetsBuilder())))
                .toList());

    assertThat(builder.apply(event, plan)).hasSize(2);
  }
}

/*
/* Copyright 2018-2024 contributors to the OpenLineage project
/* SPDX-License-Identifier: Apache-2.0
*/

package io.openlineage.spark.agent.util;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;

@Slf4j
public class SparkSessionUtils {

  public static Optional<SparkSession> activeSession() {
    try {
      return Optional.of(SparkSession.active());
    } catch (IllegalStateException e) {
      log.debug("Cannot obtain active spark session", e);
      return Optional.empty();
    }
  }
}

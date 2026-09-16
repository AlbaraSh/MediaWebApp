package com.mediawebapp.external.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RawgRatingConverterTest {

	@Test
	void toTenPointScale_doublesAndRoundsHalfUpToOneDecimal() {
		assertThat(RawgRatingConverter.toTenPointScale(4.47)).isEqualTo(8.9);
		assertThat(RawgRatingConverter.toTenPointScale(4.42)).isEqualTo(8.8);
		assertThat(RawgRatingConverter.toTenPointScale(5.0)).isEqualTo(10.0);
		assertThat(RawgRatingConverter.toTenPointScale(0.0)).isEqualTo(0.0);
		assertThat(RawgRatingConverter.toTenPointScale(null)).isNull();
	}
}

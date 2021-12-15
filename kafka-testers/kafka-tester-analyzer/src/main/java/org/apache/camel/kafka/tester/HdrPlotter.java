/*
 * Copyright 2017 Otavio Rodolfo Piske
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.camel.kafka.tester;


import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.colors.XChartSeriesColors;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The plotter for HDR histograms
 */
@SuppressWarnings("unused")
public class HdrPlotter extends AbstractHdrPlotter {
    private static final Logger LOG = LoggerFactory.getLogger(HdrPlotter.class);

    private final String baseName;

    public HdrPlotter(final String baseName) {
        this.baseName = baseName;

        getChartProperties().setyTitle("milliseconds");
    }

    public HdrPlotter(final String baseName, final String timeUnit) {
        this.baseName = baseName;

        getChartProperties().setyTitle(timeUnit);
    }


    private void plotSingleAt(final HistogramXY histogramXY, final String fileName, SeriesData... extraYSeries)
            throws IOException {
        XYChart chart = buildCommonChart();

        /*
         * This shows only the > 90 percentile, so set te minimum
         * accordingly.
         */

        // Series
        XYSeries series = chart.addSeries(getChartProperties().getSeriesName(), histogramXY.xData, histogramXY.yData);

        series.setLineColor(XChartSeriesColors.BLUE);
        series.setMarkerColor(Color.LIGHT_GRAY);
        series.setMarker(SeriesMarkers.NONE);
        series.setLineStyle(SeriesLines.SOLID);

        for (SeriesData seriesData : extraYSeries) {
            HistogramXY extraSeriesData = convertToHistogramXY(seriesData.yData);

            chart.addSeries(seriesData.seriesName, extraSeriesData.xData, extraSeriesData.yData);
        }

        BitmapEncoder.saveBitmap(chart, fileName, BitmapEncoder.BitmapFormat.PNG);
    }

    protected void plotAll(HistogramXY histogramXY, SeriesData... extraYSeries) throws IOException {
        plotSingleAt(histogramXY, baseName + "_all.png", extraYSeries);
    }


    /**
     * Plots the HDR histogram
     *
     * @throws IOException if unable to save the bitmap file
     */
    private void plot(HistogramXY histogramXY, SeriesData... extraYSeries) throws IOException {
        if (histogramXY.xData == null || histogramXY.xData.size() == 0) {
            throw new IllegalArgumentException("The 'X' column data set is empty");
        }

        if (histogramXY.yData == null || histogramXY.yData.size() == 0) {
            throw new IllegalArgumentException("The 'Y' column data set is empty");
        }

        plotAll(histogramXY, extraYSeries);
    }

    @Override
    public void plot(final Histogram hdrData, SeriesData... extraYSeries) throws IOException {
        HistogramXY histogramXY = convertToHistogramXY(hdrData);

        plot(histogramXY, extraYSeries);

    }

    private HistogramXY convertToHistogramXY(Histogram hdrData) {
        HistogramXY histogramXY = new HistogramXY();

        Iterator<HistogramIterationValue> iterator = hdrData.recordedValues().iterator();

        while (iterator.hasNext()) {
            HistogramIterationValue value = iterator.next();

            LOG.debug("Percentile: {}", value.getPercentile());
            LOG.debug("Value: {}", value.getTotalCountToThisValue());
            LOG.trace("All data: {}", value);

            histogramXY.xData.add(value.getPercentile());
            histogramXY.yData.add(value.getValueIteratedTo());
        }

        return histogramXY;
    }
}
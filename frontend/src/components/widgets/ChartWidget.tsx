import React, { useEffect, useRef, useState } from 'react';
import { Select, Space } from 'antd';
import { LineChartOutlined } from '@ant-design/icons';
import { createChart, type IChartApi, type CandlestickData } from 'lightweight-charts';
import { Widget } from '@components/ui';
import { useQuery } from '@tanstack/react-query';
import { marketDataApi } from '@services/api';

const timeframes = [
  { value: '1m', label: '1 Min' },
  { value: '5m', label: '5 Min' },
  { value: '15m', label: '15 Min' },
  { value: '1h', label: '1 Hour' },
  { value: '1d', label: '1 Day' },
];

export const ChartWidget: React.FC = () => {
  const chartContainerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<any>(null);

  const [symbol, setSymbol] = useState('AKBNK');
  const [timeframe, setTimeframe] = useState('15m');

  const { data: ohlcvData, isLoading } = useQuery({
    queryKey: ['ohlcv', symbol, timeframe],
    queryFn: () => marketDataApi.getOHLCV(symbol, timeframe),
    refetchInterval: 60000, // Refresh every minute
  });

  // Initialize chart
  useEffect(() => {
    if (!chartContainerRef.current) return;

    const chart = createChart(chartContainerRef.current, {
      width: chartContainerRef.current.clientWidth,
      height: 400,
      layout: {
        background: { color: '#ffffff' },
        textColor: '#333',
      },
      grid: {
        vertLines: { color: '#f0f0f0' },
        horzLines: { color: '#f0f0f0' },
      },
      timeScale: {
        timeVisible: true,
        secondsVisible: false,
      },
    });

    // Create series using the correct v5 API
    const candlestickSeries = (chart as any).addCandlestickSeries?.({
      upColor: '#52c41a',
      downColor: '#ff4d4f',
    }) || chart.addSeries({
      type: 'Line',
      color: '#667eea',
      lineWidth: 2,
    } as any);

    chartRef.current = chart;
    seriesRef.current = candlestickSeries;

    // Handle resize
    const handleResize = () => {
      if (chartContainerRef.current) {
        chart.applyOptions({
          width: chartContainerRef.current.clientWidth,
        });
      }
    };

    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      chart.remove();
    };
  }, []);

  // Update chart data
  useEffect(() => {
    if (!seriesRef.current || !ohlcvData) return;

    const formattedData: CandlestickData[] = ohlcvData.map((candle: any) => ({
      time: candle.time / 1000, // Convert to seconds
      open: candle.open,
      high: candle.high,
      low: candle.low,
      close: candle.close,
    }));

    seriesRef.current.setData(formattedData);
    chartRef.current?.timeScale().fitContent();
  }, [ohlcvData]);

  return (
    <Widget
      title="Price Chart"
      icon={<LineChartOutlined />}
      extra={
        <Space>
          <Select
            value={timeframe}
            onChange={setTimeframe}
            options={timeframes}
            size="small"
            style={{ width: 100 }}
          />
          <Select
            value={symbol}
            onChange={setSymbol}
            size="small"
            style={{ width: 120 }}
            showSearch
            placeholder="Symbol"
            options={[
              { value: 'AKBNK', label: 'AKBNK' },
              { value: 'THYAO', label: 'THYAO' },
              { value: 'GARAN', label: 'GARAN' },
              { value: 'ISCTR', label: 'ISCTR' },
              { value: 'EREGL', label: 'EREGL' },
            ]}
          />
        </Space>
      }
      loading={isLoading}
    >
      <div
        ref={chartContainerRef}
        className="w-full"
        style={{ minHeight: 400 }}
      />
    </Widget>
  );
};

export default ChartWidget;

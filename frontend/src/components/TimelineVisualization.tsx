// components/TimelineVisualization.tsx
import React, { useRef, useEffect, useState } from 'react';
import * as d3 from 'd3';
import { Card, Form, Button, Badge, Row, Col } from 'react-bootstrap';
import { LogEntry } from '../types/LogEntry';
import { useTheme } from '../contexts/ThemeContext';

interface TimelineEntry {
  resourceKey: string;
  section: 'plan' | 'apply' | 'other';
  resourceType?: string;
  startTime: Date;
  endTime: Date;
  durationMs: number;
  status: 'success' | 'error' | 'warning';
  entries: LogEntry[];
  levelCounts: { [key: string]: number };
}

interface TimelineVisualizationProps {
  entries: LogEntry[];
  onEntryClick: (entry: TimelineEntry) => void;
  onRequestChainSelect: (resourceKey: string) => void;
}


const TimelineVisualization: React.FC<TimelineVisualizationProps> = ({
  entries,
  onEntryClick,
  onRequestChainSelect
}) => {
  const svgRef = useRef<SVGSVGElement>(null);
  const { theme } = useTheme();
  const [selectedStatus, setSelectedStatus] = useState<string>('all');
  const [selectedResourceType, setSelectedResourceType] = useState<string>('all');
  const [timeRange, setTimeRange] = useState<string>('all');
  const [hoveredEntry, setHoveredEntry] = useState<TimelineEntry | null>(null);

  // Цветовые схемы для светлой и тёмной темы
  const colors = React.useMemo(() => {
    const isDark = theme === 'dark';

    return {
      // Основные цвета для статусов (остаются яркими в обеих темах)
      status: {
        success: '#10b981',
        warning: '#f59e0b',
        error: '#ef4444'
      },
      // Фон и текст адаптируются под тему
      background: isDark ? '#1a1d23' : '#ffffff',
      text: isDark ? '#e9ecef' : '#212529',
      textMuted: isDark ? '#adb5bd' : '#6c757d',
      // Сетка и оси
      grid: isDark ? '#495057' : '#dee2e6',
      axis: isDark ? '#ced4da' : '#495057',
      // Карточки и бордеры
      cardBackground: isDark ? '#2b3035' : '#f8f9fa',
      border: isDark ? '#495057' : '#dee2e6',
      // Ховер эффекты
      hover: isDark ? '#ffeeba' : '#fff3cd'
    };
  }, [theme]);

  const { timelineEntries, resourceTypes, statistics, planApplyPeriods } = React.useMemo(() => {
    const periods: { type: 'plan' | 'apply'; startTime: Date; endTime: Date }[] = [];
    let currentPeriod: { type: 'plan' | 'apply'; startTime: Date } | null = null;

    entries.forEach(entry => {
      if (entry.section === 'plan' && currentPeriod?.type !== 'plan') {
        if (currentPeriod) {
          periods.push({ ...currentPeriod, endTime: new Date(entry.timestamp) });
        }
        currentPeriod = { type: 'plan', startTime: new Date(entry.timestamp) };
      } else if (entry.section === 'apply' && currentPeriod?.type !== 'apply') {
        if (currentPeriod) {
          periods.push({ ...currentPeriod, endTime: new Date(entry.timestamp) });
        }
        currentPeriod = { type: 'apply', startTime: new Date(entry.timestamp) };
      }
    });

    // Завершаем последний период
    if (currentPeriod && entries.length > 0) {
      periods.push({ ...currentPeriod, endTime: new Date(entries[entries.length - 1].timestamp) });
    }

    const grouped = entries.reduce((acc, entry) => {
      const resourceKey = entry.tfResourceType || 'unknown';
      const section = entry.section || 'other';
      // Ключ: ресурс + секция
      const groupKey = `${resourceKey}|${section}`;
      if (!acc[groupKey]) {
        acc[groupKey] = [];
      }
      acc[groupKey].push(entry);
      return acc;
    }, {} as Record<string, LogEntry[]>);


    const timelineData = Object.entries(grouped).map(([groupKey, groupEntries]) => {
      const [resourceKey, section] = groupKey.split('|');
      const timestamps = groupEntries
        .filter(e => e.timestamp)
        .map(e => new Date(e.timestamp).getTime());

      if (timestamps.length === 0) return null;

      const startTime = new Date(Math.min(...timestamps));
      const endTime = new Date(Math.max(...timestamps));
      const durationMs = endTime.getTime() - startTime.getTime();

      const levelCounts = groupEntries.reduce((acc, entry) => {
        acc[entry.level] = (acc[entry.level] || 0) + 1;
        return acc;
      }, {} as { [key: string]: number });

      const hasErrors = groupEntries.some(e => e.level === 'ERROR');
      const hasWarnings = groupEntries.some(e => e.level === 'WARN');

      let status: 'success' | 'error' | 'warning' = 'success';
      if (hasErrors) status = 'error';
      else if (hasWarnings) status = 'warning';

      return {
        resourceKey,
        section: section as 'plan' | 'apply' | 'other',
        resourceType: resourceKey !== 'unknown' ? resourceKey : undefined,
        startTime,
        endTime,
        durationMs,
        status,
        entries: groupEntries,
        levelCounts
      };
    }).filter(Boolean) as TimelineEntry[];

    const stats = {
      totalChains: timelineData.length,
      errorChains: timelineData.filter(d => d.status === 'error').length,
      warningChains: timelineData.filter(d => d.status === 'warning').length,
      avgDuration: timelineData.length > 0 ? timelineData.reduce((sum, d) => sum + d.durationMs, 0) / timelineData.length : 0,
      maxDuration: timelineData.length > 0 ? Math.max(...timelineData.map(d => d.durationMs)) : 0
    };

    const resources = Array.from(new Set(timelineData.map(d => d.resourceType).filter(Boolean))) as string[];

    return {
      timelineEntries: timelineData,
      resourceTypes: resources,
      statistics: stats,
      planApplyPeriods: periods
    };
  }, [entries]);

  // Фильтрация
  const filteredEntries = timelineEntries.filter(entry => {
    const statusMatch = selectedStatus === 'all' || entry.status === selectedStatus;
    const resourceMatch = selectedResourceType === 'all' || entry.resourceType === selectedResourceType;

    let timeMatch = true;
    if (timeRange !== 'all') {
      const now = Date.now();
      const entryTime = entry.startTime.getTime();
      const ranges = {
        '5min': 5 * 60 * 1000,
        '30min': 30 * 60 * 1000,
        '1hour': 60 * 60 * 1000
      };
      timeMatch = now - entryTime <= (ranges[timeRange as keyof typeof ranges] || 0);
    }

    return statusMatch && resourceMatch && timeMatch;
  });

  useEffect(() => {
    if (!svgRef.current || filteredEntries.length === 0) return;

    const svg = d3.select(svgRef.current);
    svg.selectAll("*").remove();

    const margin = { top: 40, right: 200, bottom: 60, left: 300 };
    const width = 1200 - margin.left - margin.right;
    const height = Math.max(400, filteredEntries.length * 25);

    svg.attr('width', width + margin.left + margin.right)
      .attr('height', height + margin.top + margin.bottom)
      .style('background-color', colors.background)
      .style('border-radius', '4px');

    const g = svg.append('g').attr('transform', `translate(${margin.left},${margin.top})`);

    // Scales
    const xScale = d3.scaleTime()
      .domain(d3.extent(filteredEntries.flatMap(d => [d.startTime, d.endTime])) as [Date, Date])
      .range([0, width])
      .nice();

    const yScale = d3.scaleBand()
      .domain(Array.from(new Set(filteredEntries.map(d => d.resourceKey))))
      .range([0, height])
      .padding(0.05);

    const durationScale = d3.scaleLinear()
      .domain([0, d3.max(filteredEntries, d => d.durationMs) || 1000])
      .range([0.3, 1]);

    // Background grid
    g.append('g')
      .attr('class', 'grid')
      .attr('transform', `translate(0,${height})`)
      .call(d3.axisBottom(xScale)
        .tickSize(-height)
        .tickFormat(() => '')
      )
      .style('stroke-dasharray', '3,3')
      .style('opacity', 0.3)
      .style('stroke', colors.grid);

    // Вертикальные маркеры периодов PLAN/APPLY (ДО отрисовки столбцов)
    planApplyPeriods.forEach(period => {
      g.append('line')
        .attr('class', 'period-marker')
        .attr('x1', xScale(period.startTime))
        .attr('x2', xScale(period.startTime))
        .attr('y1', 0)
        .attr('y2', height)
        .style('stroke', period.type === 'plan' ? '#3b82f6' : '#10b981')
        .style('stroke-width', 2)
        .style('stroke-dasharray', '5,5')
        .style('opacity', 0.7);

      // Подписи периодов
      g.append('text')
        .attr('x', xScale(period.startTime))
        .attr('y', -10)
        .attr('text-anchor', 'middle')
        .style('fill', period.type === 'plan' ? '#3b82f6' : '#10b981')
        .style('font-size', '10px')
        .style('font-weight', 'bold')
        .text(period.type.toUpperCase());
    });
    // Bars
    // Для каждого ресурса создаем подгруппы по секциям
    const resourceGroups = g.selectAll('.resource-group')
      .data(Array.from(new Set(filteredEntries.map(d => d.resourceKey))))
      .enter()
      .append('g')
      .attr('class', 'resource-group')
      .attr('transform', d => `translate(0, ${yScale(d)!})`);

    resourceGroups.each(function (resourceKey) {
      const resourceEntries = filteredEntries.filter(d => d.resourceKey === resourceKey);
      const sectionScale = d3.scaleBand()
        .domain(['plan', 'apply', 'other'])
        .range([0, yScale.bandwidth()])
        .padding(0.02);

      const resourceGroup = d3.select(this);

      resourceEntries.forEach(entry => {
        const barGroup = resourceGroup.append('g')
          .attr('class', 'timeline-bar-group')
          .attr('transform', `translate(0, ${sectionScale(entry.section)!})`);

        // Цвета для секций
        const sectionColors = {
          plan: '#3b82f6',    // синий для PLAN
          apply: '#10b981',   // зеленый для APPLY  
          other: '#6b7280'    // серый для OTHER
        };

        barGroup.append('rect')
          .attr('class', 'timeline-bar')
          .attr('x', xScale(entry.startTime))
          .attr('width', Math.max(2, xScale(entry.endTime) - xScale(entry.startTime)))
          .attr('height', sectionScale.bandwidth())
          .attr('fill', sectionColors[entry.section]) // используем цвет секции вместо статуса
          .attr('opacity', durationScale(entry.durationMs))
          .attr('rx', 2)
          .style('cursor', 'pointer')
          .style('transition', 'all 0.2s ease')
          .on('click', (event) => onEntryClick(entry))
          .on('mouseover', (event) => {
            setHoveredEntry(entry);
            d3.select(event.currentTarget)
              .attr('stroke', colors.hover)
              .attr('stroke-width', 2)
              .style('filter', 'brightness(1.2)');
          })
          .on('mouseout', (event) => {
            setHoveredEntry(null);
            d3.select(event.currentTarget)
              .attr('stroke', null)
              .style('filter', 'brightness(1)');
          });

        // Подписи длительности (только для достаточно широких столбцов)
        if ((xScale(entry.endTime) - xScale(entry.startTime)) > 40) {
          barGroup.append('text')
            .attr('x', xScale(entry.startTime) + (xScale(entry.endTime) - xScale(entry.startTime)) / 2)
            .attr('y', sectionScale.bandwidth() / 2)
            .attr('text-anchor', 'middle')
            .attr('dy', '0.35em')
            .style('font-size', '9px')
            .style('font-weight', 'bold')
            .style('pointer-events', 'none')
            .style('fill', '#ffffff')
            .text(`${entry.durationMs}ms`);
        }
      });
    });

    // Axes
    const xAxis = g.append('g')
      .attr('transform', `translate(0,${height})`)
      .call(d3.axisBottom(xScale)
        .tickFormat(d3.timeFormat('%H:%M:%S') as any)
      );

    xAxis.selectAll('text')
      .style('fill', colors.text)
      .style('font-size', '11px');

    xAxis.selectAll('path, line')
      .style('stroke', colors.axis);

    xAxis.append('text')
      .attr('x', width / 2)
      .attr('y', 35)
      .style('fill', colors.text)
      .style('text-anchor', 'middle')
      .style('font-size', '12px')
      .text('Time');

    const yAxis = g.append('g')
      .call(d3.axisLeft(yScale));

    yAxis.selectAll('text')
      .style('fill', colors.text)
      .style('font-size', '9px')
      .style('cursor', 'pointer')
      .on('click', (event, resourceKey) => onRequestChainSelect(resourceKey));

    yAxis.selectAll('path, line')
      .style('stroke', colors.axis);

    // Legend
    const legend = g.append('g')
      .attr('transform', `translate(${width + 20}, 0)`);

    legend.selectAll('.legend-item')
      .data(['plan', 'apply', 'other'])
      .enter()
      .append('g')
      .attr('class', 'legend-item')
      .attr('transform', (d, i) => `translate(0, ${i * 25})`)
      .each(function (d) {
        const g = d3.select(this);
        const sectionColors = {
          plan: '#3b82f6',
          apply: '#10b981',
          other: '#6b7280'
        };

        g.append('rect')
          .attr('width', 15)
          .attr('height', 15)
          .attr('fill', sectionColors[d as keyof typeof sectionColors])
          .attr('rx', 2);
        g.append('text')
          .attr('x', 20)
          .attr('y', 12)
          .style('font-size', '12px')
          .style('fill', colors.text)
          .text(d.toUpperCase());
      });

  }, [filteredEntries, onEntryClick, onRequestChainSelect, colors, planApplyPeriods]);

  if (timelineEntries.length === 0) {
    return (
      <Card className="mt-4">
        <Card.Body className="text-muted text-center">
          No request chains found for timeline visualization
        </Card.Body>
      </Card>
    );
  }

  return (
    <Card className="mt-4">
      <Card.Header style={{
        backgroundColor: colors.cardBackground,
        borderBottom: `1px solid ${colors.border}`
      }}>
        <Row className="align-items-center">
          <Col>
            <h5 className="mb-0" style={{ color: colors.text }}>
              Request Timeline Analysis
            </h5>
          </Col>
          <Col xs="auto">
            <Badge bg="primary" className="me-2">Chains: {statistics.totalChains}</Badge>
            <Badge bg="danger" className="me-2">Errors: {statistics.errorChains}</Badge>
            <Badge bg="warning" className="me-2">Warnings: {statistics.warningChains}</Badge>
            <Badge bg="info">Avg: {Math.round(statistics.avgDuration)}ms</Badge>
          </Col>
        </Row>
      </Card.Header>

      <Card.Body style={{ backgroundColor: colors.background }}>
        {/* Панель фильтров */}
        <Row className="mb-3 g-2">
          <Col md={3}>
            <Form.Group>
              <Form.Label style={{ color: colors.text }}>Status</Form.Label>
              <Form.Select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                size="sm"
                style={{
                  backgroundColor: colors.cardBackground,
                  color: colors.text,
                  borderColor: colors.border
                }}
              >
                <option value="all">All Statuses</option>
                <option value="success">Success</option>
                <option value="warning">Warning</option>
                <option value="error">Error</option>
              </Form.Select>
            </Form.Group>
          </Col>
          <Col md={3}>
            <Form.Group>
              <Form.Label style={{ color: colors.text }}>Resource Type</Form.Label>
              <Form.Select
                value={selectedResourceType}
                onChange={(e) => setSelectedResourceType(e.target.value)}
                size="sm"
                style={{
                  backgroundColor: colors.cardBackground,
                  color: colors.text,
                  borderColor: colors.border
                }}
              >
                <option value="all">All Resources</option>
                {resourceTypes.map(type => (
                  <option key={type} value={type}>{type}</option>
                ))}
              </Form.Select>
            </Form.Group>
          </Col>
          <Col md={3}>
            <Form.Group>
              <Form.Label style={{ color: colors.text }}>Time Range</Form.Label>
              <Form.Select
                value={timeRange}
                onChange={(e) => setTimeRange(e.target.value)}
                size="sm"
                style={{
                  backgroundColor: colors.cardBackground,
                  color: colors.text,
                  borderColor: colors.border
                }}
              >
                <option value="all">All Time</option>
                <option value="5min">Last 5 min</option>
                <option value="30min">Last 30 min</option>
                <option value="1hour">Last 1 hour</option>
              </Form.Select>
            </Form.Group>
          </Col>
          <Col md={3} className="d-flex align-items-end">
            <Button
              variant="outline-secondary"
              size="sm"
              style={{
                borderColor: colors.border,
                color: colors.text
              }}
              onClick={() => {
                setSelectedStatus('all');
                setSelectedResourceType('all');
                setTimeRange('all');
              }}
            >
              Reset Filters
            </Button>
          </Col>
        </Row>

        {/* Всплывающая подсказка */}
        {hoveredEntry && (
          <Card className="mb-3" style={{
            borderColor: colors.status[hoveredEntry.status],
            backgroundColor: colors.cardBackground
          }}>
            <Card.Body className="py-2">
              <Row>
                <Col>
                  <strong style={{ color: colors.text }}>
                    Resource: {hoveredEntry.resourceKey}
                  </strong>
                  {hoveredEntry.resourceType && (
                    <span style={{ color: colors.textMuted }} className="ms-2">
                      ({hoveredEntry.resourceType})
                    </span>
                  )}
                </Col>
                <Col xs="auto">
                  <Badge bg={hoveredEntry.status === 'error' ? 'danger' : hoveredEntry.status === 'warning' ? 'warning' : 'success'}>
                    {hoveredEntry.status}
                  </Badge>
                </Col>
              </Row>
              <Row className="mt-1">
                <Col>
                  <small style={{ color: colors.textMuted }}>
                    Duration: {hoveredEntry.durationMs}ms |
                    Entries: {hoveredEntry.entries.length} |
                    Levels: {Object.entries(hoveredEntry.levelCounts).map(([level, count]) =>
                      `${level}:${count}`
                    ).join(', ')}
                  </small>
                </Col>
              </Row>
            </Card.Body>
          </Card>
        )}

        {/* Визуализация */}
        <div className="d3-timeline-container" style={{
          overflow: 'auto',
          border: `1px solid ${colors.border}`,
          borderRadius: '4px'
        }}>
          <svg ref={svgRef}></svg>
        </div>

        {/* Легенда анализа */}
        <div className="mt-3 p-2 rounded" style={{
          backgroundColor: colors.cardBackground,
          border: `1px solid ${colors.border}`
        }}>
          <small style={{ color: colors.textMuted }}>
            <strong>Analysis Tips:</strong>
            • Longer bars = longer request duration |
            • Red = errors in chain |
            • Orange = warnings in chain |
            • Click bars for details |
            • Click request IDs to filter
          </small>
        </div>
      </Card.Body>
    </Card>
  );
};

export default TimelineVisualization;
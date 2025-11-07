import React, { type ReactNode } from 'react';
import RGL, { WidthProvider, type Layout as GridLayoutType } from 'react-grid-layout';
import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';

const ReactGridLayout = WidthProvider(RGL);

export interface GridLayoutProps {
  children: ReactNode;
  layout: GridLayoutType[];
  onLayoutChange?: (layout: GridLayoutType[]) => void;
  isDraggable?: boolean;
  isResizable?: boolean;
  cols?: number;
  rowHeight?: number;
  className?: string;
}

export const GridLayout: React.FC<GridLayoutProps> = ({
  children,
  layout,
  onLayoutChange,
  isDraggable = true,
  isResizable = true,
  cols = 12,
  rowHeight = 60,
  className = '',
}) => {
  return (
    <ReactGridLayout
      className={className}
      layout={layout}
      onLayoutChange={onLayoutChange}
      cols={cols}
      rowHeight={rowHeight}
      isDraggable={isDraggable}
      isResizable={isResizable}
      draggableHandle=".widget-drag-handle"
      compactType={null}
      preventCollision={true}
      margin={[16, 16]}
      containerPadding={[0, 0]}
      useCSSTransforms={true}
    >
      {children}
    </ReactGridLayout>
  );
};

export default GridLayout;

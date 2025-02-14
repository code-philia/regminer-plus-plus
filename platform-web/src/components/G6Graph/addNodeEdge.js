import G6, { Minimap } from '@antv/g6';
import insertCss from 'insert-css';

insertCss(`
  .g6-minimap-container {
    border: 1px solid #e2e2e2;
  }
  .g6-minimap-viewport {
    border: 2px solid rgb(25, 128, 255);
  }
`);

const drawAddNodeGraph = function (json_data, graphId) {
  /**
   * 该案例演示切换交互模式，在不同模式下实现拖动节点、增加节点、增加边的交互行为。
   */
  let addedCount = 0;
  let smellType = json_data['smellType'];
  const CYCLIC_DEPENDENCY = 'CyclicDependency';
  const UNUSED_INCLUDE = 'UnusedInclude';

  // Register a custom behavior: add a node when user click the blank part of canvas
  // G6.registerBehavior('click-add-node', {
  //   // Set the events and the corresponding responsing function for this behavior
  //   getEvents() {
  //     // The event is canvas:click, the responsing function is onClick
  //     return {
  //       'canvas:click': 'onClick',
  //     };
  //   },
  //   // Click event
  //   onClick(ev) {
  //     // eslint-disable-next-line @typescript-eslint/no-this-alias
  //     const self = this;
  //     const graph = self.graph;
  //     // Add a new node
  //     graph.addItem('node', {
  //       x: ev.canvasX,
  //       y: ev.canvasY,
  //       id: `node-${addedCount}`, // Generate the unique id
  //     });
  //     addedCount++;
  //   },
  // });
  // Register a custom behavior: click two end nodes to add an edge
  G6.registerBehavior('click-add-edge', {
    // Set the events and the corresponding responsing function for this behavior
    getEvents() {
      return {
        'node:click': 'onClick', // The event is canvas:click, the responsing function is onClick
        mousemove: 'onMousemove', // The event is mousemove, the responsing function is onMousemove
        'edge:click': 'onEdgeClick', // The event is edge:click, the responsing function is onEdgeClick
      };
    },
    // The responsing function for node:click defined in getEvents
    onClick(ev) {
      // eslint-disable-next-line @typescript-eslint/no-this-alias
      const self = this;
      const node = ev.item;
      const graph = self.graph;
      // The position where the mouse clicks
      const point = { x: ev.x, y: ev.y };
      const model = node.getModel();
      if (self.addingEdge && self.edge) {
        graph.updateItem(self.edge, {
          target: model.id,
        });

        self.edge = null;
        self.addingEdge = false;
      } else {
        // Add anew edge, the end node is the current node user clicks
        self.edge = graph.addItem('edge', {
          source: model.id,
          target: model.id,
        });
        self.addingEdge = true;
      }
    },
    // The responsing function for mousemove defined in getEvents
    onMousemove(ev) {
      // eslint-disable-next-line @typescript-eslint/no-this-alias
      const self = this;
      // The current position the mouse clicks
      const point = { x: ev.x, y: ev.y };
      if (self.addingEdge && self.edge) {
        // Update the end node to the current node the mouse clicks
        self.graph.updateItem(self.edge, {
          target: point,
        });
      }
    },
    // The responsing function for edge:click defined in getEvents
    onEdgeClick(ev) {
      // eslint-disable-next-line @typescript-eslint/no-this-alias
      const self = this;
      const currentEdge = ev.item;
      if (self.addingEdge && self.edge === currentEdge) {
        self.graph.removeItem(self.edge);
        self.edge = null;
        self.addingEdge = false;
      }
    },
  });

  const container = document.getElementById(graphId);

  // Add a selector to DOM
  const selector = document.createElement('select');
  selector.id = 'selector';
  const selection1 = document.createElement('option');
  selection1.value = 'default';
  selection1.innerHTML = 'Default Mode';
  const selection2 = document.createElement('option');
  selection2.value = 'removeEdge';
  selection2.innerHTML = 'Remove Edge(By clicking Edge)';
  const selection3 = document.createElement('option');
  selection3.value = 'addEdge';
  selection3.innerHTML = 'Add Edge (By clicking two end nodes)';
  selector.appendChild(selection1);
  // selector.appendChild(selection2);
  selector.appendChild(selection3);

  const button = document.createElement('button');
  button.id = 'commit_comfirm';
  button.innerHTML = 'Save Changes';

  container.appendChild(selector);
  container.appendChild(button);

  const width = container.scrollWidth;
  const height = (container.scrollHeight || 500) - 30;

  const nodeTip = new G6.Tooltip({
    offsetX: 10,
    offsetY: -15,
    fixToNode: [1, 0],
    trigger: 'click',
    itemTypes: ['node'],
    getContent: (e) => {
      const outDiv = document.createElement('div');
      outDiv.style.width = 'fit-content';
      outDiv.innerHTML = `
              <h5>name: ${e.item.getModel().name}</h5>
              <h5>path: ${e.item.getModel().path}</h5>`;
      return outDiv;
    },
  });
  const edgeTip = new G6.Tooltip({
    offsetX: 10,
    offsetY: 0,
    fixToNode: [1, 0],
    trigger: 'click',
    itemTypes: ['edge'],
    getContent: (e) => {
      const outDiv = document.createElement('div');
      switch (smellType) {
        case CYCLIC_DEPENDENCY:
          let dependsOnTypes = e.item.getModel().dependsOnTypes;
          let str = ``;
          for (let key in dependsOnTypes) {
            str += `<ul><li>${key}: ${dependsOnTypes[key]}</li></ul>`;
          }
          outDiv.style.width = 'fit-content';
          outDiv.innerHTML =
            `
                          <h5>Source: (${e.item.getModel().source_label})${
              e.item.getModel().source_name
            }</h5>
                          <h5>Target: (${e.item.getModel().target_label})${
              e.item.getModel().target_name
            }</h5>
                          <h5>Relation: DependsOn(${e.item.getModel().times})</h5>` + str;
          break;
        case UNUSED_INCLUDE:
          outDiv.style.width = 'fit-content';
          outDiv.innerHTML = `
              <h5>Source: (${e.item.getModel().source_label})${e.item.getModel().source_name}</h5>
              <h5>Target: (${e.item.getModel().target_label})${e.item.getModel().target_name}</h5>
              <h5>Relation: Include</h5>`;
          break;
        default:
          break;
      }
      return outDiv;
    },
  });
  const removeEdgeActoin = new G6.registerBehavior('remove', {})
  const graph = new G6.Graph({
    container: graphId,
    width,
    height,
    // The sets of behavior modes
    fitView: true,
    modes: {
      // Defualt mode
      default: ['drag-canvas', 'drag-node', 'drag-combo', 'collapse-expand-combo', 'zoom-canvas'],
      // Adding node mode
      // addNode: ['click-add-node', 'click-select'],
      // Adding edge mode
      addEdge: ['click-add-edge', 'click-select'],
      re: [''],
      // onEdgeClick: ['']
    },
    plugins: [nodeTip, edgeTip, Minimap],
    defaultNode: {
      type: 'rect',
      size: 50,
      style: {
        width: 50,
        height: 20,
        lineWidth: 2,
        stroke: '#5B8FF9',
        fill: '#C6E5FF',
      },
    },
    defaultEdge: {
      type: 'quadratic',
      size: 1,
      style: {
        endArrow: {
          path: 'M 0,0 L 8,4 L 8,-4 Z',
          fill: '#A9A9A9',
        },
      },
      labelCfg: {
        autoRotate: true,
      },
    },
    layout: {
      type: 'dagre',
      rankdir: 'LR',
      align: 'DL',
      nodesepFunc: () => 1,
      ranksepFunc: () => 1,
    },
    animate: true,
    // The node styles in different states
    nodeStateStyles: {
      coreNode: {
        lineWidth: 2,
        stroke: '#DC143C',
        fill: '#FFC0CB',
      },
      // The node styles in selected state
      selected: {
        stroke: '#666',
        lineWidth: 2,
        fill: 'steelblue',
      },
    },
  });
  graph.data(json_data);
  graph.render();

  // Listen to the selector, change the mode when the selector is changed
  selector.addEventListener('change', (e) => {
    const value = e.target.value;
    // change the behavior mode
    graph.setMode(value);
  });

  graph.on('change', (e) => {
    const data = graph.get('data');
    graph.updateBehavior('click-select', {})
  });

  button.addEventListener('click', (e) => {
    // const data = graph.data();
    const data = graph.get('data');
    console.log(data);
  });

  if (typeof window !== 'undefined')
    window.onresize = () => {
      if (!graph || graph.get('destroyed')) return;
      if (!container || !container.scrollWidth || !container.scrollHeight) return;
      graph.changeSize(container.scrollWidth, container.scrollHeight - 30);
    };
};

export { drawAddNodeGraph };

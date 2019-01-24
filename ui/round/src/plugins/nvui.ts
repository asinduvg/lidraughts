import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import sanWriter from './sanWriter';
import RoundController from '../ctrl';
import { renderClock } from '../clock/clockView';
import { renderInner as tableInner } from '../view/table';
import { render as renderGround } from '../ground';
import renderCorresClock from '../corresClock/corresClockView';
import { renderResult } from '../view/replay';
import { plyStep } from '../round';
import { Step, DecodedDests, Position, Redraw } from '../interfaces';
import { Player } from 'game';
import { pos2key } from 'draughtsground/util';
import { Style, renderSan, styleSetting } from 'nvui/draughts';
import { renderSetting } from 'nvui/setting';

type Sans = {
  [key: string]: Uci;
}

type Notification = {
  text: string;
  date: Date;
}

window.lidraughts.RoundNVUI = function(redraw: Redraw) {

  let notification: Notification | undefined;

  function notify(msg: string) {
    notification = { text: msg, date: new Date() };
    window.lidraughts.requestIdleCallback(redraw);
  }

  const moveStyle = styleSetting();

  window.lidraughts.pubsub.on('socket.in.message', line => {
    if (line.u === 'lidraughts') notify(line.t);
  });
  window.lidraughts.pubsub.on('round.suggestion', notify);

  return {
    render(ctrl: RoundController) {
      const d = ctrl.data,
        step = plyStep(d, ctrl.ply),
        style = moveStyle.get();
      return ctrl.draughtsground ? h('div.nvui', [
        h('h1', 'Textual representation'),
        h('h2', 'Game info'),
        ...(['white', 'black'].map((color: Color) => h('p', [
          color + ' player: ',
          renderPlayer(ctrl, ctrl.playerByColor(color))
        ]))),
        h('p', `${d.game.rated ? 'Rated' : 'Casual'} ${d.game.perf}`),
        d.clock ? h('p', `Clock: ${d.clock.initial / 60} + ${d.clock.increment}`) : null,
        h('h2', 'Moves'),
        h('p.moves', {
          attrs: {
            role : 'log',
            'aria-live': 'off'
          }
        }, movesHtml(d.steps.slice(1), style)),
        h('h2', 'Pieces'),
        h('div.pieces', piecesHtml(ctrl)),
        h('h2', 'Game status'),
        h('div.status', {
          attrs: {
            role : 'status',
            'aria-live' : 'assertive',
            'aria-atomic' : true
          }
        }, [ctrl.data.game.status.name === 'started' ? 'Playing' : renderResult(ctrl)]),
        h('h2', 'Last move'),
        h('p.lastMove', {
          attrs: {
            'aria-live' : 'assertive',
            'aria-atomic' : true
          }
        }, renderSan(step.san, style)),
        ...(ctrl.isPlaying() ? [
          h('h2', 'Move form'),
          h('form', {
            hook: {
              insert(vnode) {
                const el = vnode.elm as HTMLFormElement;
                const d = ctrl.data;
                const $form = $(el).submit(function() {
                  const input = $form.find('.move').val();
                  const legalUcis = destsToUcis(ctrl.draughtsground.state.movable.dests!);
                const sans: Sans = sanWriter(plyStep(d, ctrl.ply).fen, legalUcis, ctrl.draughtsground.state.movable.captLen) as Sans;
                  const uci = sanToUci(input, sans) || input;
                  if (legalUcis.indexOf(uci.toLowerCase()) >= 0) ctrl.socket.send("move", {
                    from: uci.substr(0, 2),
                    to: uci.substr(2, 2)
                  }, { ackable: true });
                  else notify(d.player.color === d.game.player ? `Invalid move: ${input}` : 'Not your turn');
                  $form.find('.move').val('');
                  return false;
                });
                $form.find('.move').val('').focus();
              }
            }
          }, [
            h('label', [
              d.player.color === d.game.player ? 'Your move' : 'Waiting',
              h('input.move', {
                attrs: {
                  name: 'move',
                  'type': 'text',
                  autocomplete: 'off',
                  autofocus: true
                }
              })
            ])
          ])
        ]: []),
        h('h2', 'Your clock'),
        h('div.botc', anyClock(ctrl, 'bottom')),
        h('h2', 'Opponent clock'),
        h('div.topc', anyClock(ctrl, 'top')),
        h('div.notify', {
          attrs: {
            'aria-live': 'assertive',
            'aria-atomic' : true
          }
        }, (notification && notification.date.getTime() > (Date.now() - 1000 * 3)) ? notification.text : ''),
        h('h2', 'Actions'),
        h('div.actions', tableInner(ctrl)),
        h('h2', 'Board table'),
        h('div.board', tableBoard(ctrl)),
        h('h2', 'Settings'),
        h('label', [
          'Move notation',
          renderSetting(moveStyle, ctrl.redraw)
        ])
      ]) : renderGround(ctrl);
    }
  };
}

function anyClock(ctrl: RoundController, position: Position) {
  const d = ctrl.data, player = ctrl.playerAt(position);
  return (ctrl.clock && renderClock(ctrl, player, position)) || (
    d.correspondence && renderCorresClock(ctrl.corresClock!, ctrl.trans, player.color, position, d.game.player)
  ) || undefined;
}

function destsToUcis(dests: DecodedDests) {
  const ucis: string[] = [];
  Object.keys(dests).forEach(function(orig) {
    dests[orig].forEach(function(dest) {
      ucis.push(orig + dest);
    });
  });
  return ucis;
}

function sanToUci(san: string, sans: Sans): string | undefined {
  if (san in sans) return sans[san];
  if (san.length === 4 && Object.keys(sans).find(key => sans[key] === san)) return san;
  let lowered = san.toLowerCase().replace('x0', 'x').replace('-0', '-');
  if (lowered.slice(0, 1) === '0') lowered = lowered.slice(1)
  if (lowered in sans) return sans[lowered];
  return undefined
}

function rolePlural(r: String, c: number) {
  if (r === 'man') return c > 1 ? 'men' : 'man';
  else return c > 1 ? r + 's' : r;
}

function movesHtml(steps: Step[], style: Style) {
  const res: Array<string | VNode> = [];
  steps.forEach(s => {
    res.push(renderSan(s.san, style) + ', ');
    if (s.ply % 2 === 0) res.push(h('br'));
  });
  return res;
}

function piecesHtml(ctrl: RoundController): VNode {
  const pieces = ctrl.draughtsground.state.pieces;
  return h('div', ['white', 'black'].map(color => {
    const lists: any = [];
    ['king', 'man'].forEach(role => {
      const keys = [];
      for (let key in pieces) {
        if (pieces[key]!.color === color && pieces[key]!.role === role) keys.push(key);
      }
      if (keys.length) lists.push([rolePlural(role, keys.length), ...keys.sort().map(key => key[0] === '0' ? key.slice(1) : key)]);
    });
    return h('div', [
      h('h3', `${color} pieces`),
      ...lists.map((l: any) =>
        `${l[0]}: ${l.slice(1).join(', ')}`
      ).join(', ')
    ]);
  }));
}

/*
      1     2     3     4     5
   -  M  -  M  -  M  -  M  -  M  
6  M  -  M  -  M  -  M  -  M  -  
   -  M  -  M  -  M  -  M  -  M  
16 M  -  M  -  M  -  M  -  M  -  
   -  +  -  +  -  +  -  +  -  +
26 +  -  +  -  +  -  +  -  +  -
   -  m  -  m  -  m  -  m  -  m  
36 m  -  m  -  m  -  m  -  m  -  
   -  m  -  m  -  m  -  m  -  m  
46 m  -  m  -  m  -  m  -  m  -  
   46    47    48    49    50
 */
const filesTop = [' ', '1', ' ', '2', ' ', '3', ' ', '4', ' ', '5'],
      filesBottom = ['46', '', '47', '', '48', '', '49', '', '50'];
const ranks = ['  ', ' 6', '  ', '16', '  ', '26', '  ', '36', '  ', '46'],
      ranksInv = [' 5', '  ', '15', '  ', '25', '  ', '35', '  ', '45', '  '];
const letters = { man: 'm', king: 'k', ghostman: 'x', ghostking: 'x' };

function tableBoard(ctrl: RoundController): VNode {
  const pieces = ctrl.draughtsground.state.pieces, white = ctrl.data.player.color === 'white';
  const board = [white ? ['  ', ...filesTop] : [...filesTop, '  ']];
  for(let y = 1; y <= 10; y++) {
    let line = [];
    for(let x = 0; x < 10; x++) {
      const piece = (x % 2 !== y % 2) ? undefined : pieces[pos2key([(x - y % 2) / 2 + 1, y])];
      if (piece) {
        const letter = letters[piece.role];
        line.push(piece.color === 'white' ? letter.toUpperCase() : letter);
      } else line.push((x % 2 !== y % 2) ? '-' : '+');
    }
    board.push(white ? ['' + ranks[y - 1], ...line] : [...line, '' + ranksInv[y - 1]]);
  }
  board.push(white ? ['  ', ...filesBottom] : [...filesBottom, ' ', '  ']);
  if (!white) {
    board.reverse();
    board.forEach(r => r.reverse());
  }
  return h('table', [
    h('thead', h('tr', board[0].map(x => h('th', x)))),
    h('tbody', board.slice(1, 11).map(row =>
      h('tr', [
        h('th', row[0]),
        ...row.slice(1, 11).map(sq => h('td', sq))
      ])
    )),
    h('thead', h('tr', board[11].map(x => h('th', x))))
  ]);
}

function renderPlayer(ctrl: RoundController, player: Player) {
  return player.ai ? ctrl.trans('aiNameLevelAiLevel', 'Stockfish', player.ai) : userHtml(ctrl, player);
}

function userHtml(ctrl: RoundController, player: Player) {
  const d = ctrl.data,
    user = player.user,
    perf = user ? user.perfs[d.game.perf] : null,
    rating = player.rating ? player.rating : (perf && perf.rating),
    rd = player.ratingDiff,
    ratingDiff = rd ? (rd > 0 ? '+' + rd : ( rd < 0 ? '−' + (-rd) : '')) : '';
  return user ? h('span', [
    h('a', {
      attrs: { href: '/@/' + user.username }
    }, user.title ? `${user.title} ${user.username}` : user.username),
    rating ? ` ${rating}` : ``,
    ' ' + ratingDiff,
  ]) : 'Anonymous';
}

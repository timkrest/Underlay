# Copyright 2026 Timofey Krestyanov
# SPDX-License-Identifier: Apache-2.0
# Draws window-boundary.svg and window-boundary.ru.svg next to this file.
import os

W, H = 1200, 640
BG, INK, MUTE, LINE = "#111318", "#e8eaee", "#99a1af", "#2c313b"
ACT, DLG, STOP, OK = "#1c2230", "#232a3a", "#d96c4f", "#8b7cf6"
F = 'font-family="ui-sans-serif,-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif"'
MONO = ' font-family="ui-monospace,SFMono-Regular,Menlo,Consolas,monospace"'
HERE = os.path.dirname(os.path.abspath(__file__))

TEXTS = {
    "": dict(
        title="Why a modifier can't blur what's behind a dialog",
        sub="A Compose Dialog isn't a Box on top of the screen. It's a second window, and the feed lives in the first.",
        act="Activity window", act_sub="its own ViewRootImpl, surface and render node tree",
        dlg="Dialog window", dlg_sub1="a second android.view.Window:", dlg_sub2="its own ViewRootImpl, surface and render node tree",
        mod="Modifier.blur()", mod_sub1="or any in-window blur library:", mod_sub2="they blur what this window drew",
        stop1="stops at the window edge,", stop2="the activity layer is out of reach",
        around="Two ways around it, from outside the composition",
        one="Ask the system", one_a="FLAG_BLUR_BEHIND + blurBehindRadius", one_b="API 31+, and only while cross-window blur is on",
        two="Copy the other window yourself", two_a="PixelCopy.request(activity.window, bitmap)",
        two_b="API 26+, decorView.draw() below that", two_c="downscale ×4, blur it, draw it under the dialog",
        foot1="Both live outside the modifier.", foot2="Underlay picks one per device, at runtime.",
    ),
    ".ru": dict(
        title="Почему модификатор не заблюрит фон за диалогом",
        sub="Dialog в Compose — не Box поверх экрана. Это второе окно, а лента живёт в первом.",
        act="Окно активити", act_sub="свой ViewRootImpl, поверхность и дерево render node",
        dlg="Окно диалога", dlg_sub1="второй android.view.Window:", dlg_sub2="свой ViewRootImpl, поверхность и дерево render node",
        mod="Modifier.blur()", mod_sub1="или любая in-window библиотека:", mod_sub2="они размывают то, что нарисовало это окно",
        stop1="упирается в край окна,", stop2="до слоя активити не дотянуться",
        around="Два пути в обход, снаружи композиции",
        one="Попросить систему", one_a="FLAG_BLUR_BEHIND + blurBehindRadius", one_b="API 31+, и только пока межоконный блюр включён",
        two="Снять чужое окно самому", two_a="PixelCopy.request(activity.window, bitmap)",
        two_b="API 26+, ниже decorView.draw()", two_c="уменьшить ×4, размыть, нарисовать под диалогом",
        foot1="Оба живут вне модификатора.", foot2="Underlay выбирает на устройстве, в рантайме.",
    ),
}

def draw(t):
    o = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" {F}>',
         '<defs>',
         f'<marker id="a" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="{OK}"/></marker>',
         f'<marker id="s" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="{STOP}"/></marker>',
         '</defs>',
         f'<rect width="{W}" height="{H}" fill="{BG}"/>']

    def text(x, y, s, size=14, fill=INK, weight="400", anchor="start", mono=False):
        o.append(f'<text x="{x}" y="{y}" fill="{fill}" font-size="{size}" font-weight="{weight}" text-anchor="{anchor}"{MONO if mono else ""}>{s}</text>')

    def tree(x, y, color):  # one root, two children: a render node tree of its own
        o.append(f'<g stroke="{color}" stroke-width="1.5" fill="{color}">'
                 f'<line x1="{x}" y1="{y}" x2="{x - 11}" y2="{y + 14}"/><line x1="{x}" y1="{y}" x2="{x + 11}" y2="{y + 14}"/>'
                 f'<circle cx="{x}" cy="{y}" r="4"/><circle cx="{x - 11}" cy="{y + 14}" r="4"/><circle cx="{x + 11}" cy="{y + 14}" r="4"/></g>')

    AX, AY, AW, AH = 80, 150, 520, 420
    o.append(f'<rect x="{AX}" y="{AY}" width="{AW}" height="{AH}" rx="14" fill="{ACT}" stroke="{LINE}" stroke-width="1.5"/>')
    text(AX + 20, AY - 14, t["act"], 16, INK, "600")
    text(AX + 20, AY + 30, t["act_sub"], 13, MUTE)
    tree(AX + AW - 34, AY + 16, MUTE)
    cols = ["#4c6ef5", "#12b886", "#f59f00", "#e64980"]
    for r in range(4):
        for c in range(3):
            x, y = AX + 24 + c * 160, AY + 52 + r * 88
            o.append(f'<rect x="{x}" y="{y}" width="146" height="72" rx="8" fill="{cols[(r + c) % 4]}" opacity="0.85"/>')

    DX, DY, DW, DH = 330, 272, 400, 266
    o.append(f'<rect x="{DX + 8}" y="{DY + 10}" width="{DW}" height="{DH}" rx="14" fill="#000" opacity="0.35"/>')
    o.append(f'<rect x="{DX}" y="{DY}" width="{DW}" height="{DH}" rx="14" fill="{DLG}" stroke="{OK}" stroke-width="2"/>')
    text(DX + 20, DY + 32, t["dlg"], 16, OK, "600")
    tree(DX + DW - 34, DY + 20, OK)
    text(DX + 20, DY + 54, t["dlg_sub1"], 13, MUTE)
    text(DX + 20, DY + 72, t["dlg_sub2"], 13, MUTE)
    o.append(f'<rect x="{DX + 20}" y="{DY + 90}" width="{DW - 40}" height="98" rx="10" fill="#2c3448" stroke="{LINE}"/>')
    text(DX + 38, DY + 120, t["mod"], 16, INK, "500", mono=True)
    text(DX + 38, DY + 146, t["mod_sub1"], 13, MUTE)
    text(DX + 38, DY + 166, t["mod_sub2"], 13, MUTE)
    ax = DX + 60
    o.append(f'<line x1="{ax}" y1="{DY + 190}" x2="{ax}" y2="{DY + DH - 30}" stroke="{STOP}" stroke-width="2.5" marker-end="url(#s)"/>')
    o.append(f'<line x1="{DX + 2}" y1="{DY + DH - 22}" x2="{DX + DW - 2}" y2="{DY + DH - 22}" stroke="{STOP}" stroke-width="3"/>')
    text(ax + 22, DY + DH - 54, t["stop1"], 13, STOP)
    text(ax + 22, DY + DH - 36, t["stop2"], 13, STOP)

    RX = 790
    text(RX, 170, t["around"], 16, INK, "600")
    o.append(f'<line x1="{RX}" y1="{184}" x2="{W - 60}" y2="{184}" stroke="{LINE}"/>')
    text(RX, 224, "1", 24, OK, "700")
    text(RX + 30, 224, t["one"], 16, INK, "600")
    text(RX + 30, 250, t["one_a"], 13, MUTE, mono=True)
    text(RX + 30, 270, t["one_b"], 13, MUTE)
    o.append(f'<path d="M{RX + 10},292 C {RX - 50},235 {RX - 70},190 {AX + AW + 4},{AY + 40}" fill="none" stroke="{OK}" stroke-width="2" stroke-dasharray="6 5" marker-end="url(#a)"/>')
    text(RX, 352, "2", 24, OK, "700")
    text(RX + 30, 352, t["two"], 16, INK, "600")
    text(RX + 30, 378, t["two_a"], 13, MUTE, mono=True)
    text(RX + 30, 398, t["two_b"], 13, MUTE)
    text(RX + 30, 418, t["two_c"], 13, MUTE)
    o.append(f'<path d="M{RX + 10},438 C {RX - 40},330 {RX - 45},200 {AX + AW + 4},{AY + 65}" fill="none" stroke="{OK}" stroke-width="2" marker-end="url(#a)"/>')
    text(RX, 486, t["foot1"], 14, MUTE)
    text(RX, 506, t["foot2"], 14, MUTE)

    text(80, 70, t["title"], 28, INK, "600")
    text(80, 100, t["sub"], 15, MUTE)
    text(80, H - 28, "github.com/timkrest/Underlay", 12, "#5c6472")
    o.append("</svg>")
    return "\n".join(o)

for suffix, t in TEXTS.items():
    open(os.path.join(HERE, f"window-boundary{suffix}.svg"), "w").write(draw(t))
    print("written", f"window-boundary{suffix}.svg")

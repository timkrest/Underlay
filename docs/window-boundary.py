# Copyright 2026 Timofey Krestyanov
# SPDX-License-Identifier: Apache-2.0
import os

W, H = 1200, 640
BG, INK, MUTE, LINE = "#111318", "#e8eaee", "#99a1af", "#2c313b"
ACT, DLG, STOP, OK = "#1c2230", "#232a3a", "#d96c4f", "#8b7cf6"
F = 'font-family="ui-sans-serif,-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif"'
o = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" {F}>',
     f'<defs><marker id="a" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">'
     f'<path d="M0,0 L10,5 L0,10 z" fill="{OK}"/></marker>'
     f'<marker id="s" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">'
     f'<path d="M0,0 L10,5 L0,10 z" fill="{STOP}"/></marker></defs>',
     f'<rect width="{W}" height="{H}" fill="{BG}"/>']
def text(x, y, s, size=14, fill=INK, weight="400", anchor="start", mono=False):
    fam = ' font-family="ui-monospace,SFMono-Regular,Menlo,Consolas,monospace"' if mono else ""
    o.append(f'<text x="{x}" y="{y}" fill="{fill}" font-size="{size}" font-weight="{weight}" text-anchor="{anchor}"{fam}>{s}</text>')

AX, AY, AW, AH = 80, 150, 520, 420
o.append(f'<rect x="{AX}" y="{AY}" width="{AW}" height="{AH}" rx="14" fill="{ACT}" stroke="{LINE}" stroke-width="1.5"/>')
text(AX + 20, AY - 14, "Activity window", 15, INK, "600")
text(AX + 20, AY + 28, "its own ViewRootImpl, surface and render node tree", 12, MUTE)
cols = ["#4c6ef5", "#12b886", "#f59f00", "#e64980"]
for r in range(4):
    for c in range(3):
        x, y = AX + 24 + c * 160, AY + 52 + r * 88
        o.append(f'<rect x="{x}" y="{y}" width="146" height="72" rx="8" fill="{cols[(r + c) % 4]}" opacity="0.85"/>')

DX, DY, DW, DH = 330, 270, 400, 260
o.append(f'<rect x="{DX + 8}" y="{DY + 10}" width="{DW}" height="{DH}" rx="14" fill="#000" opacity="0.35"/>')
o.append(f'<rect x="{DX}" y="{DY}" width="{DW}" height="{DH}" rx="14" fill="{DLG}" stroke="{OK}" stroke-width="2"/>')
text(DX + 20, DY + 30, "Dialog window", 15, OK, "600")
text(DX + 20, DY + 50, "a second android.view.Window: its own ViewRootImpl, surface", 12, MUTE)
text(DX + 20, DY + 66, "and render node tree", 12, MUTE)
o.append(f'<rect x="{DX + 20}" y="{DY + 84}" width="{DW - 40}" height="90" rx="10" fill="#2c3448" stroke="{LINE}"/>')
text(DX + 40, DY + 114, "Modifier.blur()  ·  haze  ·  Cloudy", 14, INK, "500", mono=True)
text(DX + 40, DY + 138, "all of them blur inside this window", 12, MUTE)
ax = DX + 60
o.append(f'<line x1="{ax}" y1="{DY + 174}" x2="{ax}" y2="{DY + DH - 30}" stroke="{STOP}" stroke-width="2.5" marker-end="url(#s)"/>')
o.append(f'<line x1="{DX + 2}" y1="{DY + DH - 22}" x2="{DX + DW - 2}" y2="{DY + DH - 22}" stroke="{STOP}" stroke-width="3"/>')
text(ax + 24, DY + DH - 36, "stops at the window edge; the activity layer is out of reach", 12, STOP)

RX = 790
text(RX, 170, "Around the boundary, from outside the composition", 15, INK, "600")
o.append(f'<line x1="{RX}" y1="182" x2="{W - 60}" y2="182" stroke="{LINE}"/>')
text(RX, 218, "1", 22, OK, "700")
text(RX + 28, 218, "Ask the system", 15, INK, "600")
text(RX + 28, 240, "FLAG_BLUR_BEHIND + blurBehindRadius on the", 12, MUTE, mono=True)
text(RX + 28, 256, "overlay's LayoutParams. API 31+, and only while", 12, MUTE)
text(RX + 28, 272, "isCrossWindowBlurEnabled is true.", 12, MUTE, mono=True)
o.append(f'<path d="M{RX + 10},290 C {RX - 80},310 {RX - 60},{AY + 50} {AX + AW + 4},{AY + 40}" fill="none" stroke="{OK}" stroke-width="2" stroke-dasharray="6 5" marker-end="url(#a)"/>')
text(RX, 350, "2", 22, OK, "700")
text(RX + 28, 350, "Capture the other window yourself", 15, INK, "600")
text(RX + 28, 372, "PixelCopy.request(activity.window, bitmap)", 12, MUTE, mono=True)
text(RX + 28, 388, "API 26+; decorView.draw() below that.", 12, MUTE)
text(RX + 28, 404, "Downscale ×4, blur it, draw it under the dialog.", 12, MUTE)
o.append(f'<path d="M{RX + 10},420 C {RX - 80},440 {RX - 60},{AY + 90} {AX + AW + 4},{AY + 80}" fill="none" stroke="{OK}" stroke-width="2" marker-end="url(#a)"/>')
text(RX, 470, "Neither one is a modifier. Both are what", 13, MUTE)
text(RX, 488, "Underlay picks between, per device, at runtime.", 13, MUTE)

text(80, 70, "Why a modifier can't blur what's behind a dialog", 27, INK, "600")
text(80, 100, "A Compose Dialog isn't a Box on top of the screen. It's a second window, and the feed lives in the first.", 15, MUTE)
text(80, H - 28, "github.com/timkrest/Underlay", 12, "#5c6472")
o.append("</svg>")
open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "window-boundary.svg"), "w").write("\n".join(o))
print("svg written")

# MDF Server Utils

MDF Official Serverで使われるUtilities

## Command

``/mdf-util <サブコマンド> <引数>``

### サブコマンド

#### ``log-entity-removes [trace|file|none]``

Entity, TileEntityの削除をログに流す機能の設定

`trace`: traceログとしてfmlのログに出力

`file`: 別ファイルに出力

`none`: 出力しない

#### ``find-all-tiles <regexp-of-fqn>`` ``find-all-tiles <regexp-of-fqn> <start x> <start y> <start z> <end x> <end y> <end z>``

TileEntityをクラス名から探す

## LICENSE

MIT License

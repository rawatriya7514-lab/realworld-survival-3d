extends Node
class_name AudioManager

var _music_volume := 0.3
var _sfx_volume := 0.7

func _ready():
	print("AudioManager ready")

func play_music(path: String):
	var player = AudioStreamPlayer.new()
	player.stream = load(path)
	player.play()
	add_child(player)

func play_sfx(path: String):
	var player = AudioStreamPlayer.new()
	player.stream = load(path)
	player.volume_db = _sfx_volume
	player.play()
	add_child(player)

func set_music_volume(vol: float):
	_music_volume = clamp(vol, 0.0, 1.0)

func set_sfx_volume(vol: float):
	_sfx_volume = clamp(vol, 0.0, 1.0)

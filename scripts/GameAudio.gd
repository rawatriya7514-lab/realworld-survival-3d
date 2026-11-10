extends Node
class_name GameAudio

var _music_volume := 0.3
var _sfx_volume := 0.7

func _ready():
	print("GameAudio ready")

func play_sound(path: String, volume: float = 1.0):
	var player = AudioStreamPlayer.new()
	player.stream = load(path)
	player.volume_db = volume * _sfx_volume
	player.play()
	add_child(player)
	player.finished.connect(player.queue_free)

func play_music(path: String):
	var player = AudioStreamPlayer.new()
	player.stream = load(path)
	player.volume_db = _music_volume
	player.loop = true
	player.play()
	add_child(player)

func stop_all():
	for child in get_children():
		if child is AudioStreamPlayer:
			child.stop()
			child.queue_free()

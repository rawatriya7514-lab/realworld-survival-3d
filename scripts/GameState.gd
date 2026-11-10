extends Node
class_name GameState

enum GameState { MENU, PLAYING, PAUSED, GAMEOVER, MISSION_COMPLETE }

var current_state: GameState = GameState.MENU
var is_paused := false

func _ready():
	print("GameState init")

func set_state(new_state: GameState):
	current_state = new_state
	match new_state:
		GameState.MENU: print("State: MENU")
		GameState.PLAYING: print("State: PLAYING")
		GameState.PAUSED: print("State: PAUSED")
		GameState.GAMEOVER: print("State: GAMEOVER")
		GameState.MISSION_COMPLETE: print("State: MISSION_COMPLETE")

func is_menu() -> bool:
	return current_state == GameState.MENU

func is_playing() -> bool:
	return current_state == GameState.PLAYING

func is_paused_state() -> bool:
	return current_state == GameState.PAUSED

func pause():
	if current_state == GameState.PLAYING:
		current_state = GameState.PAUSED
		Engine.set_pause(true)

func resume():
	if current_state == GameState.PAUSED:
		current_state = GameState.PLAYING
		Engine.set_pause(false)

func game_over(reason: String):
	current_state = GameState.GAMEOVER
	print("Game over: " + reason)

func mission_complete():
	current_state = GameState.MISSION_COMPLETE
	print("Mission complete")

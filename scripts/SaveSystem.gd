extends Node
class_name SaveSystem

const SAVE_FILENAME = "user://save_world.json"

var is_loaded := false
var save_data: Dictionary = {
	"health": 100,
	"currency": 0,
	"score": 0,
	"inventory": [],
	"completed_missions": [],
	"current_mission": -1,
	"settings": { "brightness": 1.0, "volume": 1.0, "music_volume": 1.0 }
}

func _ready():
	load()
	print("SaveSystem ready")

func load():
	var file = FileAccess.open(SAVE_FILENAME, FileAccess.READ)
	if file:
		var content = file.get_as_text()
		file.close()
		var json = JSON.new()
		var err = json.parse(content)
		if err == OK and json.data is Dictionary:
			save_data = json.data
			is_loaded = true
			print("Save loaded")
		else:
			print("Save file corrupt, using defaults")
	else:
		print("No save file found, using defaults")

func save():
	var file = FileAccess.open(SAVE_FILENAME, FileAccess.WRITE)
	if file:
		var json_str = JSON.stringify(save_data, "\t")
		file.store_string(json_str)
		file.close()
		is_loaded = true
		print("Save written")
	else:
		print("Failed to write save file")

func get(key, default=null):
	return save_data.get(key, default)

func set(key, value):
	save_data[key] = value
	save()

func get_health() -> int:
	return save_data.get("health", 100)

func set_health(h: int):
	save_data["health"] = h

func add_currency(amount: int):
	save_data["currency"] = save_data.get("currency", 0) + amount

func add_completed_mission(mission_type: int):
	var completed = save_data.get("completed_missions", [])
	if not completed.has(mission_type):
		completed.append(mission_type)
		save_data["completed_missions"] = completed

extends Node
class_name MissionManager

enum MissionType { REACH, COLLECT, FIND, SURVIVE, ELIMINATE }

const MISSION_TYPES = [
	{ type = MissionType.REACH, name = "Reach Point", description = "Reach the checkpoint.", objective = 1 },
	{ type = MissionType.COLLECT, name = "Collect Items", description = "Gather 5 resources.", objective = 5 },
	{ type = MissionType.FIND, name = "Find Object", description = "Locate the hidden item.", objective = 1 },
	{ type = MissionType.SURVIVE, name = "Survive", description = "Survive for 30 seconds.", objective = 30 },
	{ type = MissionType.ELIMINATE, name = "Eliminate Enemies", description = "Defeat 5 hostiles.", objective = 5 }
]

var current_mission: Dictionary = {}
var mission_stage: int = 0
var mission_stages: Array = []
var completed_missions: Array = []
var active: bool = false
var _current_task: String = ""

func _ready():
	print("MissionManager ready")

func get_mission_template(mt: MissionType) -> Dictionary:
	if mt >= 0 and mt < len(MISSION_TYPES):
		var t = MISSION_TYPES[mt]
		return {
			type = mt,
			name = t.name,
			description = t.description,
			progress = 0,
			objective = t.objective,
			is_complete = false
		}
	return { type = mt, name = "Unknown", progress = 0, objective = 0, is_complete = false }

func begin_mission(mt: MissionType, target_index: int = 0) -> Dictionary:
	if mt < 0 or mt >= len(MISSION_TYPES):
		print("Invalid mission type " + str(mt))
		return {}
	if completed_missions.has(mt):
		print("Mission type " + str(mt) + " already completed")
		return {}
	current_mission = get_mission_template(mt)
	mission_stage = target_index
	mission_stages = [target_index]
	completed_missions.append(mt)
	active = true
	_current_task = "Active: " + current_mission.name
	print("Mission started: " + current_mission.name)
	return current_mission

func update_progress(amount: float):
	if not active:
		return
	current_mission.progress += amount
	print("Progress: " + str(current_mission.progress) + "/" + str(current_mission.objective))
	if current_mission.progress >= current_mission.objective:
		complete_mission()

func complete_mission():
	if not active or not current_mission:
		return
	current_mission.is_complete = true
	active = false
	print("Mission completed: " + current_mission.name)
	get_tree().call_group("GameState", "mission_complete")

func fail_mission():
	active = false
	mission_stage = 0
	_current_task = "Mission failed"
	print("Mission failed")

func save_progress() -> Dictionary:
	return {
		"current_mission": current_mission,
		"mission_stage": mission_stage,
		"mission_stages": mission_stages,
		"completed_missions": completed_missions,
		"active": active,
		"current_task": _current_task
	}

func load_progress(data: Dictionary):
	current_mission = data.get("current_mission", {})
	mission_stage = data.get("mission_stage", 0)
	mission_stages = data.get("mission_stages", [])
	completed_missions = data.get("completed_missions", [])
	active = data.get("active", false)
	_current_task = data.get("current_task", "")

func ready_for_next_mission() -> bool:
	return completed_missions.size() >= len(MISSION_TYPES)

func get_current_task() -> String:
	return _current_task

func get_mission_count() -> int:
	return len(MISSION_TYPES)

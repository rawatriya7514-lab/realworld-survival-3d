extends Camera3D
class_name PlayerCamera

export var distance := 10.0
export var min_distance := 3.0
export var max_distance := 18.0
export var sensitivity := 0.5
export var vertical_enabled := true
export var horizontal_enabled := true

var current_dist := distance
var current_pitch := -45.0
var _touch_active := false
var _last_touch_pos := Vector2.ZERO

func _ready():
	Input.touch_begin.connect(_on_touch_begin)
	Input.touch_end.connect(_on_touch_end)
	Input.touch_motion.connect(_on_touch_motion)

func _process(delta: float):
	if not Engine.is_editor_hint():
		_follow_target()

func _follow_target():
	var parent = get_node_or_null("..") as CharacterBody3D
	if not parent:
		return
	var offset_x := parent.global_transform.basis.x * current_dist
	var offset_z := parent.global_transform.basis.z * current_dist
	global_transform.origin = parent.global_transform.origin + offset_x + Vector3(0, 3.0, 0) + offset_z
	global_transform.look_at(parent.global_transform.origin + Vector3(0, 1.0, 0), Vector3(0, 1, 0))

func _on_touch_begin(pos: Vector2):
	_touch_active = true
	_last_touch_pos = pos

func _on_touch_end(pos: Vector2):
	_touch_active = false

func _on_touch_motion(pos: Vector2):
	if not _touch_active:
		return
	current_dist += (pos.x - _last_touch_pos.x) * 0.1
	current_pitch += (pos.y - _last_touch_pos.y) * 0.1
	current_dist = clamp(current_dist, min_distance, max_distance)
	current_pitch = clamp(current_pitch, -80.0, 15.0)
	_last_touch_pos = pos

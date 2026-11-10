extends Node
class_name TouchControls

const JOYSTICK_SIZE := 40.0
const ACTION_BUTTON_SIZE := 80.0

var _joystick_active := false
var _joystick_pos := Vector2.ZERO
var _joystick_origin := Vector2.ZERO
var _touch_active := false
var _interface_enabled := true

func _ready():
	Input.touch_begin.connect(_on_touch_begin)
	Input.touch_end.connect(_on_touch_end)
	Input.touch_motion.connect(_on_touch_motion)

func _on_touch_begin(pos: Vector2):
	# Left side = movement joystick, right side = camera
	if pos.x < get_viewport_rect().size.x * 0.5:
		_joystick_active = true
		_joystick_origin = pos
		_joystick_pos = pos
	else:
		_touch_active = true

func _on_touch_end(pos: Vector2):
	_joystick_active = false
	_touch_active = false

func _on_touch_motion(pos: Vector2):
	if _joystick_active:
		var delta = pos - _joystick_origin
		var dist = delta.length()
		if dist > JOYSTICK_SIZE:
			delta = delta.normalized() * JOYSTICK_SIZE
		_joystick_pos = _joystick_origin + delta
	elif _touch_active:
		# Camera look handled by PlayerCamera
		pass

func get_move_vector() -> Vector2:
	if not _joystick_active:
		return Vector2.ZERO
	var delta = _joystick_pos - _joystick_origin
	var dist = delta.length()
	if dist < 5.0:
		return Vector2.ZERO
	return (delta / JOYSTICK_SIZE).clamped(1.0)

func is_action_pressed(action: String) -> bool:
	return Input.is_action_pressed(action)

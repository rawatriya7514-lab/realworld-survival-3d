extends CharacterBody3D
class_name Player

# REALWORLD SURVIVAL 3D - PARKOUR EDITION
# Kinematic parkour controller for Godot 4.x

enum ParkourState {
	STAND,
	RUN,
	SPRINT,
	JUMP,
	DOUBLE_JUMP,
	WALL_RUN_LEFT,
	WALL_RUN_RIGHT,
	LEDGE_HANG,
	MANTLING,
	VAULTING,
	SLIDING,
	SAFETY_ROLL,
	FALLING,
	HARD_LAND,
	DEAD
}

# Movement parameters
@export var walk_speed: float = 5.0
@export var run_speed: float = 7.5
@export var sprint_speed: float = 12.0
@export var wallrun_speed: float = 12.5
@export var jump_impulse: float = 9.0
@export var gravity: float = -22.0

# Survival Stats
var health: float = 100.0
var max_health: float = 100.0
var stamina: float = 100.0
var max_stamina: float = 100.0
var adrenaline: float = 0.0
var max_adrenaline: float = 100.0
var score: int = 0
var currency: int = 0

# State Tracking
var state: ParkourState = ParkourState.STAND
var is_sprinting: bool = false
var can_double_jump: bool = true
var peak_fall_y: float = 0.0
var wallrun_timer: float = 0.0
var max_wallrun_time: float = 2.0
var mantle_timer: float = 0.0
var mantle_duration: float = 0.4
var slide_timer: float = 0.0
var roll_timer: float = 0.0
var combo_count: int = 0
var combo_timer: float = 0.0

@onready var camera_3d: Camera3D = $Camera3D

func _ready() -> void:
	peak_fall_y = global_position.y

func _physics_process(delta: float) -> void:
	if state == ParkourState.DEAD:
		return

	# Combo decay
	if combo_timer > 0.0:
		combo_timer -= delta
		if combo_timer <= 0.0:
			combo_count = 0

	# Stamina handling
	if is_sprinting and velocity.length() > 0.5:
		stamina = max(0.0, stamina - 12.0 * delta)
		if stamina <= 0.0:
			is_sprinting = false
	else:
		stamina = min(max_stamina, stamina + 18.0 * delta)

	# Read inputs
	var input_dir := Input.get_vector("move_left", "move_right", "move_forward", "move_backward")
	var wish_dir := (transform.basis * Vector3(input_dir.x, 0, input_dir.y)).normalized()

	var jump_just_pressed := Input.is_action_just_pressed("jump")
	var slide_pressed := Input.is_action_pressed("crouch") or Input.is_action_pressed("slide")

	if Input.is_action_just_pressed("sprint") and stamina > 15.0:
		is_sprinting = not is_sprinting

	# State-specific logic
	match state:
		ParkourState.MANTLING:
			_update_mantling(delta)
			return
		ParkourState.SLIDING:
			_update_sliding(delta, jump_just_pressed)
			return
		ParkourState.SAFETY_ROLL:
			_update_roll(delta)
			return
		ParkourState.WALL_RUN_LEFT, ParkourState.WALL_RUN_RIGHT:
			_update_wallrun(delta, jump_just_pressed)
			return

	# Ground / Air Movement
	var target_speed := walk_speed
	if is_sprinting and stamina > 0.0:
		target_speed = sprint_speed
	elif input_dir.length() > 0.1:
		target_speed = run_speed

	var accel := 15.0 if is_on_floor() else 5.0
	velocity.x = move_toward(velocity.x, wish_dir.x * target_speed, accel * delta)
	velocity.z = move_toward(velocity.z, wish_dir.z * target_speed, accel * delta)

	# Slide trigger
	if is_on_floor() and is_sprinting and slide_pressed and input_dir.length() > 0.1:
		_start_slide()
		return

	# Jump trigger
	if jump_just_pressed:
		if is_on_floor():
			velocity.y = jump_impulse
			can_double_jump = true
			state = ParkourState.JUMP
			peak_fall_y = global_position.y
		elif can_double_jump:
			velocity.y = jump_impulse * 0.9
			can_double_jump = false
			state = ParkourState.DOUBLE_JUMP

	# Gravity
	if not is_on_floor():
		velocity.y += gravity * delta
		velocity.y = max(velocity.y, -35.0)
		if global_position.y > peak_fall_y:
			peak_fall_y = global_position.y
		_check_wallrun()
	else:
		var drop_dist := peak_fall_y - global_position.y
		if drop_dist > 5.0:
			if slide_pressed:
				_start_safety_roll()
			else:
				var dmg := (drop_dist - 5.0) * 8.0
				health = max(0.0, health - dmg)
				if health <= 0.0:
					state = ParkourState.DEAD
		peak_fall_y = global_position.y

	move_and_slide()

	if is_on_floor():
		if velocity.length() > 8.0:
			state = ParkourState.SPRINT
		elif velocity.length() > 1.0:
			state = ParkourState.RUN
		else:
			state = ParkourState.STAND
	elif state != ParkourState.DOUBLE_JUMP and state != ParkourState.JUMP:
		state = ParkourState.FALLING

func _check_wallrun() -> void:
	if is_on_wall() and is_sprinting and stamina > 15.0:
		var wall_normal := get_wall_normal()
		var forward := -transform.basis.z
		var dot := forward.cross(wall_normal).y
		if dot > 0.2:
			state = ParkourState.WALL_RUN_LEFT
			wallrun_timer = max_wallrun_time
			velocity.y = 1.0
		elif dot < -0.2:
			state = ParkourState.WALL_RUN_RIGHT
			wallrun_timer = max_wallrun_time
			velocity.y = 1.0

func _update_wallrun(delta: float, jump_pressed: bool) -> void:
	wallrun_timer -= delta
	stamina -= 14.0 * delta
	velocity.y += gravity * 0.25 * delta

	var forward := -transform.basis.z * wallrun_speed
	velocity.x = forward.x
	velocity.z = forward.z

	if jump_pressed:
		var wall_normal := get_wall_normal()
		velocity = (forward.normalized() + wall_normal * 0.8).normalized() * 11.0
		velocity.y = jump_impulse * 1.1
		state = ParkourState.JUMP
		return

	if wallrun_timer <= 0.0 or not is_on_wall() or stamina <= 0.0:
		state = ParkourState.FALLING

	move_and_slide()

func _start_slide() -> void:
	state = ParkourState.SLIDING
	slide_timer = 0.65
	stamina -= 10.0

func _update_sliding(delta: float, jump_pressed: bool) -> void:
	slide_timer -= delta
	var forward := -transform.basis.z * 11.0 * (slide_timer / 0.65)
	velocity.x = forward.x
	velocity.z = forward.z

	if jump_pressed:
		velocity.y = jump_impulse * 0.9
		state = ParkourState.JUMP
		return

	if slide_timer <= 0.0:
		state = ParkourState.RUN if is_on_floor() else ParkourState.FALLING

	move_and_slide()

func _start_safety_roll() -> void:
	state = ParkourState.SAFETY_ROLL
	roll_timer = 0.5

func _update_roll(delta: float) -> void:
	roll_timer -= delta
	var forward := -transform.basis.z * 11.5
	velocity.x = forward.x
	velocity.z = forward.z
	if roll_timer <= 0.0:
		state = ParkourState.SPRINT

	move_and_slide()

func _update_mantling(delta: float) -> void:
	mantle_timer -= delta
	if mantle_timer <= 0.0:
		state = ParkourState.STAND

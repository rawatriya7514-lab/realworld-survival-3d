extends Node
class_name InteractionSystem

const INTERACT_RANGE := 4.0

var _interaction_cooldown := 0.0

func _ready():
	print("InteractionSystem ready")

func interact(player_pos: Vector3, player_dir: Vector3):
	if _interaction_cooldown > 0:
		return
	_interaction_cooldown = 0.5

	var ray_from = player_pos
	var ray_to = ray_from + player_dir * INTERACT_RANGE
	var space_state = get_world_3d().direct_space_state
	var query = PhysicsRayQueryParameters3D.create(ray_from, ray_to)
	query.collide_with_areas = true
	var result = space_state.intersect_ray(query)

	if result:
		var collider = result["collider"]
		if collider.has_method("on_interact"):
			collider.on_interact()
			return true
	return false

func _process(delta: float):
	if _interaction_cooldown > 0:
		_interaction_cooldown -= delta

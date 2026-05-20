using ClimbBetter.Domain.Training;

namespace ClimbBetter.Application.CQRS.Climbs.GetClimbs;

public class ClimbDto
{
    public Guid Id { get; set; }
    public string? Name { get; set; }
    public ClimbType Type { get; set; }
    public Guid LocationId { get; set; }
    public int DifficultyId { get; set; }
    public int? LengthMeters { get; set; }
    public int SuggestedMoveCount { get; set; }
    public bool IsPublic { get; set; }
}
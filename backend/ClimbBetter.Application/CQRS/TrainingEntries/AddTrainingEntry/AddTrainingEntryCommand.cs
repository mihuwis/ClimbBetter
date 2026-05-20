using ClimbBetter.Domain.Training;
using MediatR;

namespace ClimbBetter.Application.CQRS.TrainingEntries.AddTrainingEntry;

public class AddTrainingEntryCommand : IRequest<Guid>
{
    public Guid TrainingSessionId { get; set; }

    public Guid ClimbId { get; set; }

    public int DifficultyId { get; set; }

    public int QualityId { get; set; }

    public AscentCategory AscentCategory { get; set; }

    public bool IsSuccessful { get; set; }

    public int? AttemptNumberOnClimb { get; set; }

    public int ExecutedMoves { get; set; }

    public int? TotalMoves { get; set; }

    public string? Notes { get; set; }

    public Guid? ClientEntryId { get; set; }
}
using MediatR;

namespace ClimbBetter.Application.CQRS.TrainingSessions.CreateTrainingSession;

public class CreateTrainingSessionCommand : IRequest<Guid>
{
    public DateTime Date { get; set; }

    public TimeSpan? Duration { get; set; }

    public Guid? LocationId { get; set; }

    public string? Goal { get; set; }

    public string? Method { get; set; }
}
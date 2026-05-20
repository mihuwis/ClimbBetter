using ClimbBetter.Domain.Interfaces;
using ClimbBetter.Domain.Training;
using MediatR;

namespace ClimbBetter.Application.CQRS.TrainingSessions.CreateTrainingSession;

public class CreateTrainingSessionCommandHandler
    : IRequestHandler<CreateTrainingSessionCommand, Guid>
{
    private readonly ITrainingSessionRepository _trainingSessionRepository;

    public CreateTrainingSessionCommandHandler(
        ITrainingSessionRepository trainingSessionRepository)
    {
        _trainingSessionRepository = trainingSessionRepository;
    }

    public async Task<Guid> Handle(
        CreateTrainingSessionCommand request,
        CancellationToken cancellationToken)
    {
        var trainingSession = new TrainingSession
        {
            Id = Guid.NewGuid(),
            Date = DateTime.SpecifyKind(request.Date, DateTimeKind.Utc),
            Duration = request.Duration,
            LocationId = request.LocationId,
            Goal = request.Goal,
            Method = request.Method
        };

        await _trainingSessionRepository.AddAsync(
            trainingSession,
            cancellationToken);

        return trainingSession.Id;
    }
}
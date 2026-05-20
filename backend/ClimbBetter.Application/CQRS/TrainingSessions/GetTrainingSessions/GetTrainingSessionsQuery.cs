using MediatR;

namespace ClimbBetter.Application.CQRS.TrainingSessions.GetTrainingSessions;

public record GetTrainingSessionsQuery : IRequest<List<TrainingSessionListItemDto>>;